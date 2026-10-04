package com.smart.chat.couple.domain.comfort;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.Set;

/**
 * 求抱抱：一次「我有点难受」的求助，只能由对方接住——自己发的抱抱自己接不算。
 * 五种感受的白名单与接住时的话术长度都在这一个聚合里。
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
    private final String feeling;
    private String note;
    private Long heldAt;

    private ComfortRequest(String id, String by, String feeling, String note, Long heldAt) {
        this.id = id;
        this.by = by;
        this.feeling = feeling;
        this.note = note;
        this.heldAt = heldAt;
    }

    /** 发出求助 */
    public static ComfortRequest ask(String by, String feeling) {
        requireFeeling(feeling);
        return new ComfortRequest(null, by, feeling, null, null);
    }

    public static ComfortRequest restore(String id, String by, String feeling, String note, Long heldAt) {
        return new ComfortRequest(id, by, feeling, note, heldAt);
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
}
