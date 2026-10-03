package com.smart.chat.couple;

import java.util.List;

/**
 * 欢笑银行内容库（批次三十五 F390-F399）。静态内容只增不改顺序。
 * 口径：幽默是关系的复利——笑到肚子疼的时刻要存档，社死满一年会自动变成好笑的事。
 * 文案按 seed（stableHash）稳定取值，无副作用。
 */
public final class CoupleLaughBank {

    private CoupleLaughBank() {
    }

    // ========== 字典 ==========

    // ========== F390 笑点存档 ==========

    /** F390 记下笑点（推对方：该你补证词）。 */
    public static String momentLine(String title, int level) {
        return "😂 新的笑点存档：「" + title + "」（好笑度 " + level + "/5）。@" + "TA 该来补一份现场证词了。";
    }

    /** F390 补现场证词（推记账人）。 */
    public static String witnessLine(String title) {
        return "🎤 「" + title + "」的现场证词已补录，这条笑点现在是双人认证的了。";
    }

    // ========== F391 每日一逗 ==========

    /** 判分后的追加语气（强撑最扎心，单独给一句）。 */
    private static String fakeHint(String verdictLabel) {
        if ("强撑的笑".equals(verdictLabel)) {
            return "强撑也是爱，下次换个梗。";
        }
        if ("没笑".equals(verdictLabel)) {
            return "没笑不丢人，冷场是喜剧的学费。";
        }
        if ("真笑了".equals(verdictLabel)) {
            return "笑了就是赚了，复利+1。";
        }
        return "";
    }

    // ========== F392 冷笑话结冰榜 ==========

    /** F392 抛冷笑话（推对方来判）。 */
    public static String jokeLine() {
        return "🧊 有人丢了条冷笑话过来，去判它结没结冰。";
    }

    /** F392 判结冰（推讲的人）。 */
    public static String frozenLine(boolean frozen, int total) {
        return frozen
                ? "🥶 判了结冰。你今年已经冻住 " + total + " 条，冷场之王在向你招手。"
                : "🔥 没结冰，这条算是过关了（当前结冰 " + total + " 条）。";
    }

    // ========== F393 尴尬回收站 ==========

    /** F393 提交社死（推对方盖章）。 */
    public static String cringeLine() {
        return "😖 TA 交了一条社死往事进回收站，去盖个「抱抱你」的章。";
    }

    /** F393 抱抱章（推社死本人）。 */
    public static String healedLine() {
        return "🫂 「抱抱你」章已盖。这条尴尬从今天起有主了——我们一起替你觉得不好意思。";
    }

    // ========== F394 快乐突袭 ==========

    // ========== F395 笑点默契考 ==========

    // ========== F396 大笑处方 ==========

    // ========== F397 幽默风格图鉴 ==========

    // ========== F398 欢乐周报 ==========

    /** F398 周报收尾句池。 */
    private static final List<String> WEEK_TAILS = List.of(
            "这周的笑声，记账了。",
            "冷场也算，复利是连本带利的。",
            "笑点这种东西，越存越便宜，越不笑越贵。",
            "本周最佳，建议下周重播。"
    );

    /** F398 欢乐周报 summary。 */
    public static String weekSummary(String week, String fromDay, String toDay, int moments, int served,
                                     int happy, int fake, int frozen, int hits, int guesses, long seed) {
        return "🎪 " + week + " 周欢乐账（" + fromDay + " ~ " + toDay + "）：存档 " + moments
                + " 条笑点，一逗上台 " + served + " 天（真笑 " + happy + "、强撑 " + fake
                + "），冻住 " + frozen + " 条冷笑话，中弹 " + hits + " 次，笑点预判对了 " + guesses
                + " 题。" + WEEK_TAILS.get((int) Math.floorMod(seed, WEEK_TAILS.size()));
    }
}
