package com.smart.chat.couple.domain.intimacy;

/**
 * 心动值的供数：六项活数据的计数，全部来自保留的 10 张卡。
 * <p>
 * 值对象——只描述「攒到了多少」，不关心怎么查出来的。取数在 application/infrastructure，
 * 加权与定级在这里，两边都不许越界。
 *
 * @param moodDays     记过心情的天数（心情日记）
 * @param bondDays     双方当天都贴过的天数（贴贴；单向不算，单向贴贴不计的口径由 application 层保证）
 * @param deedCount    好事簿条数（TA 为我做的事）
 * @param lampCount    留过灯卡片的加班预报数（只有对方留的才算）
 * @param reflectCount 补过事后复盘的暂停次数
 * @param pointEarned  积分台账累计赚分（EARN 之和）
 */
public record IntimacySource(long moodDays, long bondDays, long deedCount, long lampCount,
                             long reflectCount, long pointEarned) {

    public IntimacySource {
        requireNonNegative("moodDays", moodDays);
        requireNonNegative("bondDays", bondDays);
        requireNonNegative("deedCount", deedCount);
        requireNonNegative("lampCount", lampCount);
        requireNonNegative("reflectCount", reflectCount);
        requireNonNegative("pointEarned", pointEarned);
    }

    /** 一张卡都没动过的空档期 */
    public static IntimacySource empty() {
        return new IntimacySource(0, 0, 0, 0, 0, 0);
    }

    private static void requireNonNegative(String field, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("心动值供数 " + field + " 不能为负：" + value);
        }
    }
}
