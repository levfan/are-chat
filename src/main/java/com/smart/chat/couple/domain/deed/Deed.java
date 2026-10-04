package com.smart.chat.couple.domain.deed;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 好事簿里的一条记录：「TA 为我做的事」，由被照顾的那一方亲手写下。
 * <p>
 * 这条聚合存在的理由是三条散在 Service 里的真实规则（{@code docs/ddd/05-tactical-playbook.md} 第 1 节第 3 条）：
 * <ol>
 *   <li><b>内容闸门</b>：写一句才算一条，去掉首尾空白不能空、最长 {@value #CONTENT_MAX} 个字，
 *       发生日只收 {@code yyyy-MM-dd}，留空即今天；</li>
 *   <li><b>加星只归记录人</b>：「这条救过我」是记录人自己的重量，TA 的记录只能 TA 点
 *       （{@link #starBy}，文案就是用户看到的那句原话）；</li>
 *   <li><b>加星幂等</b>：重复点第二次既不报错也不改行——是「再点一次没坏处」，
 *       所以返回 false 让用例走早退，而不是抛异常。</li>
 * </ol>
 * 「同日同人同内容不能记两遍」这条查重需要读存储，不在聚合里判，见 {@link DeedRepository#alreadyRecorded}。
 * 加分归属（分给做事的那位）也不在这里：那是用例的记账口径，不是这条记录自身的状态。
 */
public final class Deed {

    /** 一件好事的字数上限（与 {@code couple_echo_deed.content} 的现役口径一致） */
    public static final int CONTENT_MAX = 80;

    private final String id;
    private final String spaceId;
    private final String fromUser;
    private final String content;
    private final String day;
    private boolean starred;
    private final long created;
    private Long updatedAt;

    private Deed(String id, String spaceId, String fromUser, String content, String day, boolean starred,
                 long created, Long updatedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.fromUser = fromUser;
        this.content = content;
        this.day = day;
        this.starred = starred;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /**
     * 记一条好事：清洗内容、定发生日（留空=今天）并现场发 id 与记录时刻，初始未加星。
     *
     * @param day   发生日原文，可为空
     * @param today 今天，用于 day 缺省
     * @throws RuleViolation 内容为空／超长／日期格式不对（对外 400，文案原样）
     */
    public static Deed record(String spaceId, String fromUser, String content, String day, LocalDate today) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new RuleViolation("好事总得写一句");
        }
        if (text.length() > CONTENT_MAX) {
            throw new RuleViolation("一件好事最多 " + CONTENT_MAX + " 字");
        }
        String occurredOn = day == null || day.isBlank() ? today.toString() : day.trim();
        if (!occurredOn.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new RuleViolation("日期写成 yyyy-MM-dd");
        }
        long at = System.currentTimeMillis();
        return new Deed(UUID.randomUUID().toString(), spaceId, fromUser, text, occurredOn, false, at, at);
    }

    /** 从存储重建：不校验——存量行必须读得出来，starred 为空按「没加过星」看待。 */
    public static Deed restore(String id, String spaceId, String fromUser, String content, String day,
                               Integer starred, Long created, Long updatedAt) {
        return new Deed(id, spaceId, fromUser, content, day, Integer.valueOf(1).equals(starred),
                created == null ? 0L : created, updatedAt);
    }

    /**
     * 记录人给这条证据点「这条救过我」。
     *
     * @return true=这一次真的点亮了；false=早就点过了（幂等，用例据此早退）
     * @throws RuleViolation 不是记录人（对外 400，文案原样）
     */
    public boolean starBy(String me) {
        if (!fromUser.equals(me)) {
            throw new RuleViolation("只有记下这条的人能加星");
        }
        if (starred) {
            return false;
        }
        starred = true;
        updatedAt = System.currentTimeMillis();
        return true;
    }

    /** 这条是谁记下的（加分要给「被记的那位」，用例据此找 partner）。 */
    public boolean recordedBy(String me) {
        return fromUser.equals(me);
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

    public String content() {
        return content;
    }

    public String day() {
        return day;
    }

    public boolean starred() {
        return starred;
    }

    public long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
