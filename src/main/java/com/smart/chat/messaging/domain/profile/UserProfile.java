package com.smart.chat.messaging.domain.profile;

import com.smart.chat.messaging.domain.RuleViolation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.OptionalLong;
import java.util.Set;

/**
 * 用户资料卡（{@code user_profile} 的业务名）：昵称/签名/头像色档/在线状态/生日。
 * <p>
 * 建这张表的意义就是「资料由本人说了算」，所以每个字段的取值范围都是领域规则：
 * 昵称 1~32、签名 ≤100（可以为空）、头像只能是预设色档、在线状态只有三档、
 * 生日允许只填 {@code MM-dd}（不在意年份）。改造前这些 if 全写在 Controller 里，
 * 换个人再加一个入口就会漏一条校验。
 * <p>
 * 文案与 code（400）沿用改造前，一字不动。
 */
public final class UserProfile {

    public static final int NICKNAME_MAX = 32;
    public static final int SIGNATURE_MAX = 100;
    public static final int AVATAR_MAX = 8;
    /** 在线状态只有这三档，前端按值上色 */
    public static final Set<String> PRESENCE_STATUSES = Set.of("online", "busy", "away");
    /** 未填在线状态时的显示口径 */
    public static final String PRESENCE_ONLINE = "online";
    /** 建档时的默认头像色档（与 identity 的建档适配器同口径） */
    public static final String DEFAULT_AVATAR = "c0";

    private final String username;
    private String nickname;
    private String signature;
    private String avatar;
    private String presenceStatus;
    private String birthday;
    private Long updatedAt;

    private UserProfile(String username, String nickname, String signature, String avatar, String presenceStatus,
                        String birthday, Long updatedAt) {
        this.username = username;
        this.nickname = nickname;
        this.signature = signature;
        this.avatar = avatar;
        this.presenceStatus = presenceStatus;
        this.birthday = birthday;
        this.updatedAt = updatedAt;
    }

    /**
     * 建档：老用户可能还没有资料行，第一次读到就补一行默认值。
     * 默认口径与改造前的 {@code ensureProfile} / {@code ProfileProvisionerAdapter} 完全一致
     * （签名为空串、在线状态 online、头像 c0）。
     */
    public static UserProfile provision(String username, String nickname, String avatar, long at) {
        return new UserProfile(username, nickname, "", avatar, PRESENCE_ONLINE, null, at);
    }

    /** 从存储重建：不校验，存量行里什么都有。 */
    public static UserProfile restore(String username, String nickname, String signature, String avatar,
                                      String presenceStatus, String birthday, Long updatedAt) {
        return new UserProfile(username, nickname, signature, avatar, presenceStatus, birthday, updatedAt);
    }

    /** 改昵称：空串不算改，超长也不让（前端拿这个值当显示名）。 */
    public void changeNickname(String raw) {
        String value = raw.trim();
        if (value.isEmpty() || value.length() > NICKNAME_MAX) {
            throw RuleViolation.of("昵称需为 1~" + NICKNAME_MAX + " 个字");
        }
        this.nickname = value;
    }

    /** 改签名：可以清空，最长 100。 */
    public void changeSignature(String raw) {
        String value = raw.trim();
        if (value.length() > SIGNATURE_MAX) {
            throw RuleViolation.of("签名最长 " + SIGNATURE_MAX + " 个字");
        }
        this.signature = value;
    }

    /** 改头像：只接受预设色档编号，长度超过 {@value #AVATAR_MAX} 的一定不是预设项。 */
    public void changeAvatar(String raw) {
        String value = raw.trim();
        if (value.length() > AVATAR_MAX) {
            throw RuleViolation.of("头像请从预设色档中选择");
        }
        this.avatar = value;
    }

    /** 改在线状态：只认 online / busy / away。 */
    public void changePresenceStatus(String raw) {
        String value = raw.trim();
        if (!PRESENCE_STATUSES.contains(value)) {
            throw RuleViolation.of("在线状态仅支持 online / busy / away");
        }
        this.presenceStatus = value;
    }

    /**
     * 改生日：空串表示清除；填了就必须是 {@code yyyy-MM-dd} 或 {@code MM-dd}。
     * 校验口径沿用改造前——{@code MM-dd} 借 2000 年试解析一次，且长度必须正好 5，
     * 否则 {@code 1-5} 这种「看着对」的写法也会被放过。
     */
    public void changeBirthday(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            this.birthday = null;
            return;
        }
        requireBirthdayShape(value);
        this.birthday = value;
    }

    /** 改造前的 {@code ensureProfile} 会顺手把空的在线状态填成 online（更新时随之写回）。 */
    public void fillPresenceStatusIfMissing() {
        if (presenceStatus == null) {
            presenceStatus = PRESENCE_ONLINE;
        }
    }

    public void markUpdated(long at) {
        this.updatedAt = at;
    }

    /**
     * F42 距今年生日还有几天：{@code yyyy-MM-dd} 只取月日，已经过了就顺延一年。
     * 生日没填或格式不认识时返回空——改造前这里是「跳过这一条」，不是报错。
     */
    public OptionalLong daysUntilBirthday(LocalDate today) {
        if (birthday == null || birthday.isBlank()) {
            return OptionalLong.empty();
        }
        String monthDay = birthday.length() >= 10 ? birthday.substring(5) : birthday;
        LocalDate next;
        try {
            next = LocalDate.parse(today.getYear() + "-" + monthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return OptionalLong.empty();
        }
        if (next.isBefore(today)) {
            next = next.plusYears(1);
        }
        return OptionalLong.of(ChronoUnit.DAYS.between(today, next));
    }

    private void requireBirthdayShape(String value) {
        boolean ok = false;
        try {
            LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            ok = true;
        } catch (Exception ignored) {
            // 尝试下一种格式
        }
        if (!ok) {
            try {
                LocalDate.parse("2000-" + value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                ok = value.length() == 5;
            } catch (Exception ignored) {
                // 保持 false
            }
        }
        if (!ok) {
            throw RuleViolation.of("生日格式应为 yyyy-MM-dd 或 MM-dd");
        }
    }

    public String username() {
        return username;
    }

    public String nickname() {
        return nickname;
    }

    public String signature() {
        return signature;
    }

    public String avatar() {
        return avatar;
    }

    /** 原值，可能为 null；对外显示请用 {@link #presenceStatusForView()}。 */
    public String presenceStatus() {
        return presenceStatus;
    }

    /** 没填就是 online——这是产品口径，不是兜底样式。 */
    public String presenceStatusForView() {
        return presenceStatus == null ? PRESENCE_ONLINE : presenceStatus;
    }

    public String birthday() {
        return birthday;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
