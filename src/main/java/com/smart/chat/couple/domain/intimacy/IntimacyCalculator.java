package com.smart.chat.couple.domain.intimacy;

/**
 * 心动值计算器：六项加权求和 + 七级恋爱阶梯定级。
 * <p>
 * 这是情侣空间最核心的一条产品规则，所以它 belongs 在领域层，而不是长在 {@code CoupleService} 的方法体里。
 * <b>权重与阈值是定死的口径，改任何一个数字都要先回答「会不会让某对情侣当场掉级」</b>：
 * 2026-10-04 裁剪时刻意不动阶梯阈值，就是为了避免「功能被裁了，等级跟着掉」这种观感。
 * 回归护栏是 {@code CoupleIntimacyTest}（锁权重、锁「单向贴贴不算一天」、锁 50/1300 两个边界）。
 */
public final class IntimacyCalculator {

    /** 六项权重：贴贴按「天」计，好事与复盘各 ×2，留灯最重 ×3，积分原值累加 */
    public static final long WEIGHT_MOOD_DAYS = 1;
    public static final long WEIGHT_BOND_DAYS = 2;
    public static final long WEIGHT_DEED_COUNT = 2;
    public static final long WEIGHT_LAMP_COUNT = 3;
    public static final long WEIGHT_REFLECT_COUNT = 2;
    public static final long WEIGHT_POINT_EARNED = 1;

    /** 各级起点分：L1 怦然心动(0) → L2 心动初启(50) → L3 甜甜热恋(150) → L4 形影不离(300)
     *  → L5 心有灵犀(500) → L6 相依相伴(800) → L7 相守一生(1300) */
    private static final int[] THRESHOLDS = {0, 50, 150, 300, 500, 800, 1300};
    private static final String[] TITLES = {"", "怦然心动", "心动初启", "甜甜热恋", "形影不离", "心有灵犀", "相依相伴", "相守一生"};
    private static final String[] ICONS = {"", "✨", "💫", "🍬", "🧡", "💞", "🌷", "💍"};
    /** 满级 */
    public static final int MAX_LEVEL = THRESHOLDS.length;

    private IntimacyCalculator() {
    }

    /** 六项加权求和（先按 long 算完再收窄，避免中途溢出） */
    public static int scoreOf(IntimacySource source) {
        long score = source.moodDays() * WEIGHT_MOOD_DAYS
                + source.bondDays() * WEIGHT_BOND_DAYS
                + source.deedCount() * WEIGHT_DEED_COUNT
                + source.lampCount() * WEIGHT_LAMP_COUNT
                + source.reflectCount() * WEIGHT_REFLECT_COUNT
                + source.pointEarned() * WEIGHT_POINT_EARNED;
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
