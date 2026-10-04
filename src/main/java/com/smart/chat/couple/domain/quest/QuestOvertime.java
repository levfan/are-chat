package com.smart.chat.couple.domain.quest;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.UUID;

/**
 * 加班预报与留灯：今晚忙到几点先说一声，灯卡只有对方能留——「到家灯给你留着」这句话不该由自己说。
 * <p>
 * 聚合守住三条真实规则（{@code docs/ddd/05-tactical-playbook.md} 第 1 节第 3 条）：
 * <ol>
 *   <li><b>小时钳制</b>：忙到的点在 {@value #HOUR_MIN}–{@value #HOUR_MAX} 之间，缺省落 {@value #HOUR_DEFAULT}，
 *       越界钳到边界（不报错——是宽容钳制，不是闸门）；</li>
 *   <li><b>一句说明</b>：可空、最长 {@value #NOTE_MAX} 字，原话照搬现役文案；</li>
 *   <li><b>灯只能对方留、自己留不算</b>：留灯人不能是预报本人（{@link #leaveLampBy}），灯卡必填不空白、
 *       最长 {@value #LAMP_MAX} 字。</li>
 * </ol>
 * 不可变：每次状态迁移返回新对象。每人每天一行（uk(space_id,day,from_user)）由仓储端口 upsert 落实。
 */
public final class QuestOvertime {

    public static final int HOUR_MIN = 13;
    public static final int HOUR_MAX = 23;
    public static final int HOUR_DEFAULT = 20;
    public static final int NOTE_MAX = 40;
    public static final int LAMP_MAX = 60;

    private final String id;
    private final String spaceId;
    private final String day;
    private final String fromUser;
    private final int untilHour;
    private final String note;
    private final String lamp;
    private final String lampBy;
    private final long created;
    private final Long updatedAt;

    private QuestOvertime(String id, String spaceId, String day, String fromUser, int untilHour, String note,
                          String lamp, String lampBy, long created, Long updatedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.day = day;
        this.fromUser = fromUser;
        this.untilHour = untilHour;
        this.note = note;
        this.lamp = lamp;
        this.lampBy = lampBy;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /** 新预报：钳小时、清洗说明，现场发 id；灯还没人留（lamp 空串、lampBy null）。 */
    public static QuestOvertime forecast(String spaceId, String day, String fromUser, Integer untilHour, String note) {
        int h = clampHour(untilHour);
        String n = requireNote(note);
        long now = System.currentTimeMillis();
        return new QuestOvertime(UUID.randomUUID().toString(), spaceId, day, fromUser, h, n, "", null, now, now);
    }

    /** 从存储重建：不校验——历史行必须读得出来；untilHour 为空按缺省点看。 */
    public static QuestOvertime restore(String id, String spaceId, String day, String fromUser, Integer untilHour,
                                        String note, String lamp, String lampBy, Long created, Long updatedAt) {
        return new QuestOvertime(id, spaceId, day, fromUser, untilHour == null ? HOUR_DEFAULT : untilHour,
                note, lamp, lampBy, created == null ? 0L : created, updatedAt);
    }

    /** 改写今晚的预报（同一行）：重钳小时、重校验说明，保留 id/归属/已有灯卡与 created，只刷新更新时间。 */
    public QuestOvertime reforecast(Integer untilHour, String note) {
        int h = clampHour(untilHour);
        String n = requireNote(note);
        return new QuestOvertime(id, spaceId, day, fromUser, h, n, lamp, lampBy, created, System.currentTimeMillis());
    }

    /**
     * 给对方留一张灯卡：只有加班人以外的人能留，「自己留不算」这句话就是用户看到的原话。
     *
     * @throws RuleViolation 自己是加班人／灯卡空白／超长（对外 400，文案原样）
     */
    public QuestOvertime leaveLampBy(String me, String text) {
        if (fromUser.equals(me)) {
            throw new RuleViolation("灯是给加班的人留的，自己留不算 💡");
        }
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            throw new RuleViolation("灯下想留的那句话写一句");
        }
        if (t.length() > LAMP_MAX) {
            throw new RuleViolation("灯卡最多 " + LAMP_MAX + " 字");
        }
        return new QuestOvertime(id, spaceId, day, fromUser, untilHour, note, t, me, created,
                System.currentTimeMillis());
    }

    private static int clampHour(Integer untilHour) {
        return untilHour == null ? HOUR_DEFAULT : Math.max(HOUR_MIN, Math.min(HOUR_MAX, untilHour));
    }

    private static String requireNote(String note) {
        String n = note == null ? "" : note.trim();
        if (n.length() > NOTE_MAX) {
            throw new RuleViolation("一句说明最多 " + NOTE_MAX + " 字");
        }
        return n;
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String day() {
        return day;
    }

    public String fromUser() {
        return fromUser;
    }

    public int untilHour() {
        return untilHour;
    }

    public String note() {
        return note;
    }

    public String lamp() {
        return lamp;
    }

    public String lampBy() {
        return lampBy;
    }

    public long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
