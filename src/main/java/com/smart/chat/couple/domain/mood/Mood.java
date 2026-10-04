package com.smart.chat.couple.domain.mood;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.util.Set;

/**
 * 心情日记的一行：某个空间里的某个人，在某个自然日记下的一个心情键（可带一句话）。
 * <p>
 * 这是<b>薄实体</b>：「每人每天一行、当天改写」的写入用例现在写在 {@code CoupleService.setMood} 里
 * （那不在本片的改造范围），所以这里只登记现役读法真正用到的三件事——
 * 「这一条是谁在哪天记的」（{@link #recordedBy}／{@link #moodDay}）、
 * 「那天低不低落」（{@link #isDowncast}，深夜陪伴据此决定要不要打扰对方）与
 * 「哪天」这个自然日怎么写才算数（{@link #dayOrToday}）。
 * 心情键白名单与曲线分值沿用写侧的同一套键值，这里不另立第二份口径。
 */
public final class Mood {

    /** 深夜陪伴当作「低落」的四个心情键（现网口径，动它会直接改变 night-care 的推送对象）。 */
    private static final Set<String> DOWNCAST = Set.of("SAD", "ANGRY", "SICK", "TIRED");

    private final String id;
    private final String spaceId;
    private final String username;
    private final String moodDay;
    private final String mood;
    private final String note;
    private final Long created;
    private final Long updatedAt;

    private Mood(String id, String spaceId, String username, String moodDay, String mood, String note,
                 Long created, Long updatedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.username = username;
        this.moodDay = moodDay;
        this.mood = mood;
        this.note = note;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /** 从存储重建：不校验——存量行必须读得出来。 */
    public static Mood restore(String id, String spaceId, String username, String moodDay, String mood, String note,
                               Long created, Long updatedAt) {
        return new Mood(id, spaceId, username, moodDay, mood, note, created, updatedAt);
    }

    /**
     * 心情日期的口径：留空即今天，写了就必须是 {@code yyyy-MM-dd}（改写与回应都认这一个自然日）。
     *
     * @throws RuleViolation 日期写错（对外 400，文案原样）
     */
    public static String dayOrToday(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now().toString();
        }
        try {
            return LocalDate.parse(raw.trim()).toString();
        } catch (Exception e) {
            throw new RuleViolation("日期格式应为 yyyy-MM-dd");
        }
    }

    /** 这一条是谁记的（双人曲线按人分堆用，不是业务闸门）。 */
    public boolean recordedBy(String me) {
        return username.equals(me);
    }

    /** 那天是不是低落到该让 TA 的对方知道一声。 */
    public boolean isDowncast() {
        return DOWNCAST.contains(mood);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String username() {
        return username;
    }

    public String moodDay() {
        return moodDay;
    }

    public String mood() {
        return mood;
    }

    public String note() {
        return note;
    }

    public Long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
