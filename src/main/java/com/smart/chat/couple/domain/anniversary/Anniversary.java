package com.smart.chat.couple.domain.anniversary;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.Set;
import java.util.UUID;

/**
 * 共同日历上的一天：纪念日、生日、约会日。
 * <p>
 * 三条规则住在这里：
 * <ul>
 *   <li>{@code yearly} 只有「每年重复」与「就这一回」两种，落库存 1/0；</li>
 *   <li>大日子类型（F127）只认 LOVE/FAMILY/FRIEND/WORK，认不了的一律回退 NORMAL——
 *       这不是宽容，是日历不能被脏值弄出空白格；</li>
 *   <li>历法（F253）为 LUNAR 时 {@code lunarMd} 才是真源，{@code eventDate} 只是首年换算结果。</li>
 * </ul>
 */
public final class Anniversary {

    public static final int TITLE_MAX = 60;
    public static final String KIND_NORMAL = "NORMAL";
    public static final String KIND_LOVE = "LOVE";
    public static final String KIND_FAMILY = "FAMILY";
    public static final String KIND_FRIEND = "FRIEND";
    public static final String KIND_WORK = "WORK";
    public static final String CALENDAR_SOLAR = "SOLAR";
    public static final String CALENDAR_LUNAR = "LUNAR";

    private static final Set<String> KNOWN_KINDS = Set.of(KIND_LOVE, KIND_FAMILY, KIND_FRIEND, KIND_WORK);

    private final String id;
    private final String spaceId;
    private final String title;
    private final String eventDate;
    private final boolean yearly;
    private final String kind;
    private final String calendarType;
    private final String lunarMd;
    private final String createdBy;
    private final long created;

    private Anniversary(String id, String spaceId, String title, String eventDate, boolean yearly, String kind,
                        String calendarType, String lunarMd, String createdBy, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.title = title;
        this.eventDate = eventDate;
        this.yearly = yearly;
        this.kind = kind;
        this.calendarType = calendarType;
        this.lunarMd = lunarMd;
        this.createdBy = createdBy;
        this.created = created;
    }

    /** 记一个日子：名称必填不超长，类型认不了就回退 NORMAL，农历日子另存月日 */
    public static Anniversary schedule(String spaceId, String title, String date, boolean yearly,
                                       String createdBy, String requestedKind, boolean lunar, String lunarMd) {
        String text = title == null ? "" : title.trim();
        if (text.isEmpty() || text.length() > TITLE_MAX) {
            throw new RuleViolation("纪念日名称不能为空（最多 " + TITLE_MAX + " 字）");
        }
        return new Anniversary(UUID.randomUUID().toString(), spaceId, text, date, yearly, kindOf(requestedKind),
                lunar ? CALENDAR_LUNAR : CALENDAR_SOLAR, lunar && lunarMd != null ? lunarMd.trim() : "",
                createdBy, System.currentTimeMillis());
    }

    /** 从存储还原（老行的 yearly 存的是 1/0，日历类型可能为空，都按现状读回来） */
    public static Anniversary restore(String id, String spaceId, String title, String eventDate, Integer yearly,
                                      String kind, String calendarType, String lunarMd, String createdBy,
                                      Long created) {
        return new Anniversary(id, spaceId, title, eventDate, Integer.valueOf(1).equals(yearly),
                kind == null ? KIND_NORMAL : kind, calendarType, lunarMd, createdBy,
                created == null ? 0L : created);
    }

    /** F127 大日子类型：空或非法一律 NORMAL */
    public static String kindOf(String requested) {
        return requested != null && KNOWN_KINDS.contains(requested) ? requested : KIND_NORMAL;
    }

    /** 农历日子以 lunarMd 为真源，eventDate 只是首年换算结果 */
    public boolean lunar() {
        return CALENDAR_LUNAR.equals(calendarType);
    }

    public boolean repeatsYearly() {
        return yearly;
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String title() {
        return title;
    }

    public String eventDate() {
        return eventDate;
    }

    public String kind() {
        return kind;
    }

    public String calendarType() {
        return calendarType;
    }

    public String lunarMd() {
        return lunarMd;
    }

    public String createdBy() {
        return createdBy;
    }

    public long created() {
        return created;
    }
}
