package com.smart.chat.couple.domain.comfort;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.Set;
import java.util.UUID;

/**
 * 求抱抱：一次「我有点难受」的求助，只能由对方接住——自己发的抱抱自己接不算。
 * 五种感受的白名单、接住时的话术长度、感受的中文目录（label/emoji）与「同一天再说一次」的
 * 改写规则都在这一个聚合里。
 */
public final class ComfortRequest {

    public static final String SAD = "SAD";
    public static final String WRONGED = "WRONGED";
    public static final String TIRED = "TIRED";
    public static final String ANXIOUS = "ANXIOUS";
    public static final String EMO = "EMO";
    /** 五种感受，与 CoupleComfortBank 的话术键一一对应 */
    public static final Set<String> FEELINGS = Set.of(SAD, WRONGED, TIRED, ANXIOUS, EMO);
    /** 接住时那句话的上限（100，界面上别写成 60） */
    public static final int NOTE_MAX = 100;

    private final String id;
    private final String by;
    private String feeling;
    private String note;
    private Long heldAt;
    private boolean handled;
    private final String spaceId;
    private final String day;
    private final Long created;

    private ComfortRequest(String id, String by, String feeling, String note, Long heldAt, boolean handled,
                           String spaceId, String day, Long created) {
        this.id = id;
        this.by = by;
        this.feeling = feeling;
        this.note = note;
        this.heldAt = heldAt;
        this.handled = handled;
        this.spaceId = spaceId;
        this.day = day;
        this.created = created;
    }

    /** 发出求助 */
    public static ComfortRequest ask(String by, String feeling) {
        requireFeeling(feeling);
        return new ComfortRequest(null, by, feeling, null, null, false, null, null, null);
    }

    public static ComfortRequest restore(String id, String by, String feeling, String note, Long heldAt) {
        return new ComfortRequest(id, by, feeling, note, heldAt, heldAt != null, null, null, null);
    }

    /**
     * 发出「今天」这一条求助：带上所属空间与发生日，时刻现场盖。
     * 每人每天一条是存储口径（见 {@code couple_comfort.uk_comfort_day}），
     * 「已经有就改写、没有就新建」由 {@code CoupleComfortService} 走端口实现。
     */
    public static ComfortRequest askOn(String spaceId, String by, String day, String feeling) {
        requireFeeling(feeling);
        return new ComfortRequest(UUID.randomUUID().toString(), by, feeling, null, null, false,
                spaceId, day, System.currentTimeMillis());
    }

    /**
     * 从存储的整行重建；不校验——存量行必须读得出来。
     * <p>
     * {@code handled} 取库里的 handled 列而不是 {@code heldAt} 是否为空：改写感受时 MyBatis 的
     * NOT_NULL 更新策略不会清空 handled_note / handled_at，那两列可能留着上一次被接住的残值，
     * 只有 handled 一列说了准（现役 CoupleComfortService 读的就是这一列）。
     */
    public static ComfortRequest restore(String id, String by, String feeling, String note, Long heldAt,
                                         boolean handled, String spaceId, String day, Long created) {
        return new ComfortRequest(id, by, feeling, note, heldAt, handled, spaceId, day, created);
    }

    /**
     * 同一天再说一次：感受改写，「已被接住」抹平——重新开口就得重新被接。
     *
     * @throws RuleViolation 感受不在白名单（对外 400，文案原样）
     */
    public void reask(String feeling) {
        this.feeling = requireFeeling(feeling);
        this.handled = false;
    }

    /** 这条求助是哪一天发的。 */
    public boolean raisedOn(String day) {
        return this.day == null ? false : this.day.equals(day);
    }

    /** 这一行的「已被接住」标记（看板与深夜陪伴都认这一列）。 */
    public boolean handled() {
        return handled;
    }

    /** 感受的中文名（看板 VO 与推送文案用，目录与前端逐字一致）。 */
    public String feelingLabel() {
        return switch (feeling) {
            case SAD -> "难过";
            case WRONGED -> "委屈";
            case TIRED -> "好累";
            case ANXIOUS -> "焦虑";
            case EMO -> "emo";
            default -> "不太好";
        };
    }

    /** 感受的表情（同上，目录照搬 {@code CoupleComfortPO.feelingEmoji}）。 */
    public String feelingEmoji() {
        return switch (feeling) {
            case SAD -> "😢";
            case WRONGED -> "🥺";
            case TIRED -> "😮‍💨";
            case ANXIOUS -> "😖";
            case EMO -> "🌧️";
            default -> "🫂";
        };
    }

    /** 感受键校验（取话术卡也走这条） */
    public static String requireFeeling(String feeling) {
        if (feeling == null || !FEELINGS.contains(feeling)) {
            throw new RuleViolation("感受只能是 难过/委屈/累/焦虑/emo 哦");
        }
        return feeling;
    }

    /** 取话术卡时的键校验（与「发出求助」的文案不同：一个是告诉用户怎么选，一个是说明取不到卡） */
    public static String requireKnown(String feeling) {
        if (feeling == null || !FEELINGS.contains(feeling)) {
            throw new RuleViolation("不认识这种感受哦");
        }
        return feeling;
    }

    /** 接住别人的抱抱时那句话的硬规矩：不能空、不能过长 */
    public static String requireNote(String words) {
        String text = words == null ? "" : words.trim();
        if (text.isEmpty() || text.length() > NOTE_MAX) {
            throw new RuleViolation("写一句 " + NOTE_MAX + " 字以内的话，把抱抱送过去");
        }
        return text;
    }

    /** 接住：必须是对方递过来的那句，且要写成一句人话 */
    public void hold(String words, long at) {
        String text = words == null ? "" : words.trim();
        if (text.isEmpty() || text.length() > NOTE_MAX) {
            throw new RuleViolation("写一句 " + NOTE_MAX + " 字以内的话，把抱抱送过去");
        }
        this.note = text;
        this.heldAt = at;
        this.handled = true;
    }

    public boolean isHeld() {
        return heldAt != null;
    }

    /** 这条求助是不是我发的（发的和接的不能是同一个人） */
    public boolean askedBy(String me) {
        return by.equals(me);
    }

    public String id() {
        return id;
    }

    public String by() {
        return by;
    }

    public String feeling() {
        return feeling;
    }

    public String note() {
        return note;
    }

    public Long heldAt() {
        return heldAt;
    }

    public String spaceId() {
        return spaceId;
    }

    public String day() {
        return day;
    }

    public Long created() {
        return created;
    }
}
