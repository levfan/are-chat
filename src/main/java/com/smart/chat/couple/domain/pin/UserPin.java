package com.smart.chat.couple.domain.pin;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * 收藏卡（F207）：一个人钉在页签顶部「我的常用」里的那几枚功能卡，保存是整组覆盖。
 * <p>
 * 口径来自 {@code CONTEXT.md} couple 小节「收藏卡：每人 ≤6 且 key 是前端卡根 data-testid」，
 * 全部在这一层守：键去掉首尾空白后不能为空、单键最长 {@value #KEY_MAX} 字符（再长就不是我们的卡键了）、
 * 去重后最多 {@value #MAX_PINS} 枚，点选先后顺序保留。
 * 「一个空间每人只有一行」是存储口径，见 {@link UserPinRepository#findBySpaceAndUser}。
 */
public final class UserPin {

    /** 每人最多钉几枚卡（前端网格只有六格）。 */
    public static final int MAX_PINS = 6;
    /** 卡键长度上限：现役最长的 data-testid 也远小于它，超过就是脏数据。 */
    public static final int KEY_MAX = 40;

    private final String id;
    private final String spaceId;
    private final String fromUser;
    private List<String> keys;
    private final long created;
    private Long updatedAt;

    private UserPin(String id, String spaceId, String fromUser, List<String> keys, long created, Long updatedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.fromUser = fromUser;
        this.keys = keys;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /** 还没钉过卡的人从空板开始（空板合法，不校验）。 */
    public static UserPin blank(String spaceId, String fromUser) {
        return new UserPin(UUID.randomUUID().toString(), spaceId, fromUser, List.of(),
                System.currentTimeMillis(), null);
    }

    /** 从存储重建：不校验——存量行必须读得出来（历史上可能存过超长键）。 */
    public static UserPin restore(String id, String spaceId, String fromUser, String joined, Long created,
                                  Long updatedAt) {
        return new UserPin(id, spaceId, fromUser, split(joined), created == null ? 0L : created, updatedAt);
    }

    /**
     * 整组改写我的常用：清洗、去重、按上限校验，然后记下这一次改动的时刻。
     *
     * @throws RuleViolation 单键超长／去重后超过 {@value #MAX_PINS} 枚（对外 400，文案原样）
     */
    public void replaceWith(List<String> rawKeys) {
        LinkedHashSet<String> kept = new LinkedHashSet<>();
        if (rawKeys != null) {
            for (String raw : rawKeys) {
                if (raw == null) {
                    continue;
                }
                String key = raw.trim();
                if (key.isEmpty()) {
                    continue;
                }
                if (key.length() > KEY_MAX) {
                    throw new RuleViolation("收藏键太长啦");
                }
                kept.add(key);
            }
        }
        if (kept.size() > MAX_PINS) {
            throw new RuleViolation("最多收藏 " + MAX_PINS + " 个，先放下一个再钉新的");
        }
        this.keys = List.copyOf(kept);
        this.updatedAt = System.currentTimeMillis();
    }

    /** 落库形态：逗号分隔的一列。 */
    public String joined() {
        return String.join(",", keys);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String fromUser() {
        return fromUser;
    }

    public List<String> keys() {
        return keys;
    }

    public long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }

    private static List<String> split(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return Arrays.stream(joined.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
