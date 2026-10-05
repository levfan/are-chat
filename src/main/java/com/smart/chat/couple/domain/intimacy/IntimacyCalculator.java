package com.smart.chat.couple.domain.intimacy;

/**
 * 心动值计算器：五项加权求和 + 七级恋爱阶梯定级。
 * <p>
 * 这是情侣空间最核心的一条产品规则，所以它 belongs 在领域层，而不是长在 {@code CoupleService} 的方法体里。
 * <b>权重与阈值是定死的口径，改任何一个数字都要先回答「会不会让某对情侣当场掉级」</b>——
 * 2026-10-05 二轮裁剪是例外：六项供数里有五项随功能下线归零，等级会永久卡在 L1，
 * 保留公式等于保留一个不再动的数字，所以这次连同阈值一起重标（推导过程见
 * {@code docs/adr/0010-couple-trim-to-v8-features.md} 第 3 条）。
 * 回归护栏是 {@code CoupleIntimacyTest} 与 {@code IntimacyCalculatorTest}（锁权重、锁「单向答完不算一天」、锁级差边界）。
 */
public final class IntimacyCalculator {

    /** 五项权重：在一起天数打底 ×1，打卡与最长连续各 ×2/×3，答完一题 ×3，实现一个愿望最重 ×5 */
    public static final long WEIGHT_DAYS_TOGETHER = 1;
    public static final long WEIGHT_CHECKIN_DAYS = 2;
    public static final long WEIGHT_LONGEST_STREAK = 3;
    public static final long WEIGHT_ANSWER_DAYS = 3;
    public static final long WEIGHT_WISH_FULFILLED = 5;

    /** 各级起点分：L1 怦然心动(0) → L2 心动初启(60) → L3 甜甜热恋(150) → L4 形影不离(260)
     *  → L5 心有灵犀(400) → L6 相依相伴(560) → L7 相守一生(760)。
     * 定标依据：满打满算在一起 30 天、天天答题的一对≈225 分（L3 中段），
     * 100 天且实现过几个愿望的一对≈520 分（L6 门口），一年后稳定进 L7——
     * 让「百日回顾」那天的等级有分量，又不至于三个月就封顶。 */
    private static final int[] THRESHOLDS = {0, 60, 150, 260, 400, 560, 760};
    private static final String[] TITLES = {"", "怦然心动", "心动初启", "甜甜热恋", "形影不离", "心有灵犀", "相依相伴", "相守一生"};
    private static final String[] ICONS = {"", "✨", "💫", "🍬", "🧡", "💞", "🌷", "💍"};
    /** 满级 */
    public static final int MAX_LEVEL = THRESHOLDS.length;

    private IntimacyCalculator() {
    }

    /** 五项加权求和（先按 long 算完再收窄，避免中途溢出） */
    public static int scoreOf(IntimacySource source) {
        long score = source.daysTogether() * WEIGHT_DAYS_TOGETHER
                + source.checkinDays() * WEIGHT_CHECKIN_DAYS
                + source.longestStreak() * WEIGHT_LONGEST_STREAK
                + source.answerDays() * WEIGHT_ANSWER_DAYS
                + source.wishFulfilled() * WEIGHT_WISH_FULFILLED;
        return (int) score;
    }

    /** 分数定级（负分按 L1 处理，不产生「0 级」这种界面无法表达的状态） */
    public static IntimacyLevel levelOf(int score) {
        int level = 1;
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= THRESHOLDS[i]) {
                level = i + 1;
                break;
            }
        }
        Integer next = level >= MAX_LEVEL ? null : THRESHOLDS[level];
        return new IntimacyLevel(level, TITLES[level], ICONS[level], next, progressOf(score, level));
    }

    /** 一步到位：供数 → 分数 + 等级 */
    public static Intimacy evaluate(IntimacySource source) {
        int score = scoreOf(source);
        return new Intimacy(score, levelOf(score), source);
    }

    /** 距下一级进度：以本级起点与下一级阈值插值，满级恒为 100，封顶 99（没到就不算到） */
    private static int progressOf(int score, int level) {
        if (level >= MAX_LEVEL) {
            return 100;
        }
        int floor = THRESHOLDS[level - 1];
        int span = THRESHOLDS[level] - floor;
        return span <= 0 ? 100 : (int) Math.min(99, (score - floor) * 100L / span);
    }

    /** 等级快照 */
    public record IntimacyLevel(int level, String title, String icon, Integer nextLevelAt, int progress) {
    }

    /** 计算结果：分数 + 等级 + 原始供数（供数要能随响应下发，界面上要说清「分是怎么来的」） */
    public record Intimacy(int score, IntimacyLevel level, IntimacySource source) {
    }
}
