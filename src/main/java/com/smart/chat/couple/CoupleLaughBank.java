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

    /** F394 突袭类型中文标签。 */
    public static String kindLabel(String kind) {
        return switch (kind == null ? "" : kind) {
            case CoupleLaughAttack.KIND_PRAISE -> "一串夸奖";
            case CoupleLaughAttack.KIND_MEME -> "一个梗";
            case CoupleLaughAttack.KIND_MEMORY -> "一段回忆杀";
            default -> "一次突袭";
        };
    }

    /** F391 判分中文标签。 */
    public static String verdictLabel(String verdict) {
        return switch (verdict == null ? "" : verdict) {
            case CoupleLaughDaily.VERDICT_HAPPY -> "真笑了";
            case CoupleLaughDaily.VERDICT_FLAT -> "没笑";
            case CoupleLaughDaily.VERDICT_FAKE -> "强撑的笑";
            default -> "还没判";
        };
    }

    /** F397 幽默风格中文标签。 */
    public static String styleLabel(String style) {
        return switch (style == null ? "" : style) {
            case CoupleLaughStyle.STYLE_PUN -> "谐音梗";
            case CoupleLaughStyle.STYLE_COLD -> "冷幽默";
            case CoupleLaughStyle.STYLE_SELF -> "自嘲派";
            case CoupleLaughStyle.STYLE_ACTION -> "动作派";
            case CoupleLaughStyle.STYLE_MIME -> "模仿派";
            default -> "还没测";
        };
    }

    /** F396 处方指向的中文标签。 */
    public static String targetLabel(String kind) {
        return switch (kind == null ? "" : kind) {
            case CoupleLaughRx.TARGET_MOMENT -> "一条笑点存档";
            case CoupleLaughRx.TARGET_CRINGE -> "一段社死往事";
            case CoupleLaughRx.TARGET_ATTACK -> "一次快乐突袭";
            default -> "一剂笑";
        };
    }

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

    /** F391 交内容（推判分人）。 */
    public static String dailyServeLine(String owner) {
        return "🎪 今天轮到 " + owner + " 逗笑，节目已上台——你负责判：笑了、没笑、还是强撑。";
    }

    /** F391 判分（推负责逗的人）。 */
    public static String dailyJudgeLine(String verdictLabel) {
        return "🏅 今日一逗判决：" + verdictLabel + "。" + fakeHint(verdictLabel);
    }

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

    /** F393 满一年转好笑（读时结算，推双方）。 */
    public static String turnedFunnyLine(String day) {
        return "🎉 时间到账：" + day + " 那条社死满一年了，自动转档成「好笑的事」。当时想钻地缝，现在只想重播。";
    }

    // ========== F394 快乐突袭 ==========

    /** F394 发动突袭（推对方）。 */
    public static String attackLine(String kindLabel, String content) {
        return "💥 " + kindLabel + "突袭：「" + content + "」——中弹了请盖章。";
    }

    /** F394 中弹盖章（推发动的人）。 */
    public static String hitLine(String by) {
        return "🎯 " + by + " 举手认弹：这波我确实笑了。";
    }

    // ========== F395 笑点默契考 ==========

    /** F395 双方预判一致（推双方）。 */
    public static String guessTwinLine() {
        return "🔮 笑点默契 +1：这条梗你们俩都预判对方会笑，笑点已经长到一起了。";
    }

    /** F395 预判不一致（推双方）。 */
    public static String guessDiffLine() {
        return "🤔 这条预判不一样——笑点默契的债，还得靠多聊来还。";
    }

    // ========== F396 大笑处方 ==========

    /** F396 开处方（推收方）。 */
    public static String rxLine(String targetLabel, String note) {
        String tail = note == null || note.isBlank() ? "" : "，医嘱：" + note;
        return "💊 大笑处方：翻" + targetLabel + tail + "。翻完记得回执「已服用」。";
    }

    /** F396 已服用（推开方的人）。 */
    public static String takenLine() {
        return "✅ 处方已服用，药效：笑出声。开方的那位可以放心了。";
    }

    // ========== F397 幽默风格图鉴 ==========

    /** F397 风格一致。 */
    public static String styleSameLine(String styleLabel) {
        return "🎭 你们俩的幽默类型对上了：都是「" + styleLabel + "」。笑点同频，省话。";
    }

    /** F397 风格差异的相处建议。 */
    public static String styleDiffLine(String mine, String partner) {
        return "🎭 幽默类型不一样：你是「" + mine + "」，TA 是「" + partner
                + "」。给你一句相处建议——TA 的笑点不用你懂，你负责笑就行；同理，你讲的 TA 没笑，不代表不好笑。";
    }

    /** F397 还没评的提示。 */
    public static String styleMissingLine() {
        return "🎭 风格图鉴还没填满：自评一份、再替对方评一份，差异才会给建议。";
    }

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

    // ========== F399 年度欢笑榜 ==========

    /** F399 年度称号（按笑点与中弹综合）。 */
    public static String yearTitle(int moments, int hits, int happy, int frozen) {
        int score = moments * 2 + hits * 2 + happy + frozen;
        if (score >= 60) {
            return "全年无休喜剧组合";
        }
        if (score >= 36) {
            return "彼此的快乐供应商";
        }
        if (score >= 20) {
            return "越冷越好笑的人";
        }
        if (score >= 8) {
            return "偶尔笑到打鸣";
        }
        return "还在攒第一声笑";
    }

    /** F399 年度欢笑榜 summary（bestLine 是当年最好笑那一条的标题，可为空）。 */
    public static String yearSummary(String year, int moments, int laughs, int dailyDone, int happy, int frozen,
                                     String kingOfCold, int cringe, int cringeHealed, int turns, int attacks,
                                     int hits, int guessTwin, int rxTaken, String bestLine, long seed) {
        String best = bestLine == null || bestLine.isBlank() ? "暂时选不出最好笑的一条" : "当年最好笑：「" + bestLine + "」";
        return "🏆 " + year + " 年我们的喜剧奖：" + best + "；笑点存档 " + moments + " 条（补了 "
                + laughs + " 份证词），一逗上台 " + dailyDone + " 天（真笑 " + happy + " 场），结冰 "
                + frozen + " 条——冷场之王是 " + kingOfCold + "；社死 " + cringe + " 条（" + cringeHealed
                + " 条已盖抱抱章，" + turns + " 条满一年自动转成好笑的事），突袭 " + attacks + " 次（中弹 "
                + hits + " 次），笑点默契预判对 " + guessTwin + " 题，开了 " + rxTaken + " 张已服用的大笑处方——称号「"
                + yearTitle(moments, hits, happy, frozen) + "」。";
    }
}
