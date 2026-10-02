package com.smart.chat.couple;

import java.util.List;

/**
 * 注意力保护区内容库（批次三十二 F360-F369）。静态内容只增不改顺序。
 * 口径：注意力是当代最贵的礼物，「我在看你」比「我爱你」稀缺——
 * 专注打卡、专属时段、攒一句话、饭桌不低头、对视十秒、不插电半小时、
 * 走神温柔哨、专注周报、数字排毒半天、注意力年报。
 */
public final class CoupleFocusBank {

    private CoupleFocusBank() {
    }

    // ========== F360 专注打卡 ==========

    /** F360 一方报到时的推送文案（推双方，提醒另一半也来报）。 */
    public static String nightOneSideLine(String minutes) {
        return "🌙 " + minutes + " 分钟没碰手机，TA 已经报了今晚的专注打卡，你也来报一下？";
    }

    /** F360 双方都报当夜点亮（推双方）。 */
    public static String nightLitLine(String minutes, int total) {
        return "🌙✨ 今晚点亮了！两人各 " + minutes + " 分钟，一共 " + total + " 分钟手机是黑的，TA 是亮的。";
    }

    /** F360 未点亮时的提示行（只有一方报过时挂在总览上）。 */
    public static String nightWaitingLine() {
        return "还有一个人没报，今晚的灯先留着一半 🕯️";
    }

    // ========== F362 攒一句话 ==========

    /** F362 攒下那句话（推收件人：你忙的时候我先把话放这儿）。 */
    public static String queueAddedLine(String content) {
        return "🗒️ 你刚才在忙，我先把话攒这儿了：「" + content + "」——不急，回头看就好。";
    }

    /** F362 一键收全部的回执文案（推双方，count=本次签收条数）。 */
    public static String queueReadLine(String reader, int count) {
        return "📬 " + reader + " 一口气签收了 " + count + " 句攒下的话，排队区清空啦。";
    }

    // ========== F361 专属时段 ==========

    /** F361 提议发出（推对方：等一个确认）。 */
    public static String slotPlannedLine(String title, String day, int hours) {
        return "📅 专属时段已预约：" + day + " 留 " + hours + " 小时给「" + title + "」——只属于我们，等你点头。";
    }

    /** F361 对方确认（推双方：生效了）。 */
    public static String slotConfirmedLine(String title, String day) {
        return "✅ 专属时段敲定：" + day + " 的「" + title + "」生效啦，那天手机放远点。";
    }

    // ========== F363 饭桌不低头 ==========

    /** F363 双方都点了（推双方）。 */
    public static String mealBothLine() {
        return "🍚 同桌成功：两个人都把手机倒扣了，这顿饭是热的、看着彼此吃的。";
    }

    // ========== F364 对视十秒 ==========

    /** F364 双点点亮（推双方）。 */
    public static String gazeBothLine() {
        return "👀 对视十秒达成：刚才那十秒里，世界上只有你和 TA 两个人。";
    }

    // ========== F365 不插电半小时 ==========

    /** F365 双方都打卡（推双方）。 */
    public static String unplugBothLine() {
        return "🔌 不插电半小时：两个人都关机了半小时，睡眠比消息更值得回。";
    }

    /** F365 周连击读时算出来的提示行（streak=本周连续双点天数）。 */
    public static String unplugStreakLine(int streak) {
        if (streak <= 1) {
            return "今晚开始也算数，连击从 1 起步 🔌";
        }
        return "本周已经连着 " + streak + " 晚不插电了，别断在这 🔌";
    }

    // ========== F366 走神温柔哨 ==========

    /** F366 递卡文案（note 为空时用兜底句）。 */
    public static String nudgeLine(long seed, String note) {
        if (note != null && !note.isBlank()) {
            return "🐦 「" + note + "」——说好了，回来就好。";
        }
        return NUDGE_DEFAULT.get(Math.floorMod(seed, NUDGE_DEFAULT.size()));
    }

    private static final List<String> NUDGE_DEFAULT = List.of(
            "🐦 哨子响一下，回来啦——手机上的事明天还在，我不在。",
            "🐦 抬头看一眼吧，我在这儿坐着呢。",
            "🐦 温柔提醒一次：屏幕里的热闹，没有这边的呼吸声好听。",
            "🐦 哨声很轻：走神没关系，回来就好。");

    // ========== F368 数字排毒半天 ==========

    /** F368 发起/应战的第一声（推对方）。 */
    public static String detoxStartedLine(String kindLabel, String day) {
        return "🌿 " + day + " 的" + kindLabel + "排毒半天已经挂上，手机放远点，你来应战吗？";
    }

    /** F368 双方都报=达成（推双方）。 */
    public static String detoxDoneLine(String kindLabel, String day) {
        return "🕊️ 清净半天达成：" + day + " 的" + kindLabel + "两个人都没碰手机，这份安静收下了。";
    }

    /** F368 半天代号的中文说法（AM/PM）。 */
    public static String kindLabel(String kind) {
        return CoupleFocusDetox.KIND_PM.equals(kind) ? "下半天" : "上半天";
    }

    // ========== F367 专注周报 ==========

    /** F367 周报收尾句池。 */
    public static String weekTail(long seed) {
        return WEEK_TAIL.get(Math.floorMod(seed, WEEK_TAIL.size()));
    }

    private static final List<String> WEEK_TAIL = List.of(
            "专注是可以存的，存够了这周就没白过。",
            "手机少看一会儿，人就在面前多待一会儿。",
            "下周的灯，接着留。");

    /** F367 专注周报 summary（数字全部来自真实表）。 */
    public static String weekSummary(String week, String fromDay, String toDay, int minutes, int meals,
                                     int gazes, int unplugs, int slots, int nudges, long seed) {
        return "🗓️ " + week + " 专注周报（" + fromDay + " ~ " + toDay + "）：两人一共放下手机 " + minutes
                + " 分钟，同桌 " + meals + " 次，对视 " + gazes + " 次，不插电 " + unplugs + " 晚，专属时段 "
                + slots + " 段，温柔哨 " + nudges + " 张。" + weekTail(seed);
    }

    // ========== F369 注意力年报 ==========

    /** F369 年报收尾句池。 */
    public static String yearTail(long seed) {
        return YEAR_TAIL.get(Math.floorMod(seed, YEAR_TAIL.size()));
    }

    private static final List<String> YEAR_TAIL = List.of(
            "这一年你们把注意力当礼物送出去，收回来的是彼此。",
            "手机里的世界一直有新消息，你们只有这一段人生。",
            "放下的每一分钟，都长在关系里了。");

    /** F369 注意力年报 summary（hours=把专注分钟换算成「为彼此放下的手机小时数」）。 */
    public static String yearSummary(String year, int minutes, String hours, int litNights, int meals,
                                     int gazes, int unplugs, int detox, String topDay, int topMinutes,
                                     long seed) {
        String top = topDay == null || topDay.isBlank()
                ? "还没有最专注的一天，今年还长着呢"
                : "最专注的一天是 " + topDay + "（合计 " + topMinutes + " 分钟）";
        return "📻 " + year + " 注意力年报：为彼此放下手机 " + hours + " 小时（" + minutes + " 分钟），点亮 " + litNights
                + " 个夜晚，同桌 " + meals + " 次，对视 " + gazes + " 次，不插电 " + unplugs + " 晚，清净半天 "
                + detox + " 次；" + top + "。" + yearTail(seed);
    }
}
