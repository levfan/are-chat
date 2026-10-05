package com.smart.chat.couple.domain.intimacy;

/**
 * 心动值的供数：五项活数据的计数，全部来自情侣空间现役的四件事
 * （邀请建立空间、每日一问、愿望清单、连续互动打卡）。
 * <p>
 * 值对象——只描述「攒到了多少」，不关心怎么查出来的。取数在 application/infrastructure，
 * 加权与定级在这里，两边都不许越界。
 * <p>
 * 2026-10-05 二轮裁剪把原来的六项供数（心情条数/贴贴双向往来天数/好事簿条数/留灯次数/
 * 安全词复盘次数/积分累计）整个删掉了，这五项是重建后的口径，阈值也随之重标——
 * 理由与逐项对照见 {@code docs/adr/0010-couple-trim-to-v8-features.md} 第 3 条。
 *
 * @param daysTogether  在一起的天数（纪念日缺省时按建立日算，建立当天=第 1 天）
 * @param checkinDays   累计打卡天数（双方当天都答完每日一问，或补签成功）
 * @param longestStreak 历史最长连续打卡天数（只看峰值，断签不回收）
 * @param answerDays    双方都答完每日一问的天数
 * @param wishFulfilled 已实现的愿望条数（双方合计）
 */
public record IntimacySource(long daysTogether, long checkinDays, long longestStreak, long answerDays,
                             long wishFulfilled) {

    public IntimacySource {
        requireNonNegative("daysTogether", daysTogether);
        requireNonNegative("checkinDays", checkinDays);
        requireNonNegative("longestStreak", longestStreak);
        requireNonNegative("answerDays", answerDays);
        requireNonNegative("wishFulfilled", wishFulfilled);
    }

    /** 刚建立空间、什么都还没做 */
    public static IntimacySource empty() {
        return new IntimacySource(0, 0, 0, 0, 0);
    }

    private static void requireNonNegative(String field, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("心动值供数 " + field + " 不能为负：" + value);
        }
    }
}
