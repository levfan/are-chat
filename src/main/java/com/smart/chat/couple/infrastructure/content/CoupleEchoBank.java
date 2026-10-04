package com.smart.chat.couple.infrastructure.content;

import java.util.List;

/**
 * 回音壁内容库（批次三十一 F350-F359）。静态内容只增不改顺序。
 * 口径：把「TA 爱我」的证据存下来，低落的时候取出来充电——
 * 好事簿、鼓励语罐、能量补给、感谢慢递、高光重放、夸夸回执、电量预报、写给低落的自己、被爱日历、年报。
 */
public final class CoupleEchoBank {

    private CoupleEchoBank() {
    }

    /** F350 好事簿新增推送文案（推双方）。 */
    public static String deedAddedLine(String content) {
        return "📒 好事簿又厚了一页：TA 为我做了「" + content + "」，这笔我收下了。";
    }

    /** F350 「这条救过我」加星推送文案（推双方）。 */
    public static String deedStarredLine(String content) {
        return "⭐ 「这条救过我」：" + content + "——低谷时它真的被打开过。";
    }

    /** F351 能量补给开箱文案（按种子稳定抽）。 */
    public static String refillLine(long seed) {
        return REFILLS.get(Math.floorMod(seed, REFILLS.size()));
    }

    private static final List<String> REFILLS = List.of(
            "今日份能量已到账：证据、鼓励、高光各来一点，慢慢用 ⚡",
            "充电完成。这些是真实发生过的，不是安慰话。",
            "补给领好了。低落的时候记得回来翻一翻这里 🔌",
            "今天的电量包已拆封：好事簿里的 TA，比你现在想的要爱你。",
            "领到了就先别慌：证据都在，你不是没人撑的人。");

    /** F351 补给领取推送文案（detail 带 actor，推双方）。 */
    public static String refillPushLine(String actor) {
        return "⚡ " + actor + " 刚领了今天的能量补给，你也来充一次？";
    }

    /** F357 对方低电量提示行池（都会带「今晚轻轻的」，读时挂在对方的电量格上）。 */
    public static String lowBatteryLine(long seed) {
        return LOW_BATTERY.get(Math.floorMod(seed, LOW_BATTERY.size()));
    }

    private static final List<String> LOW_BATTERY = List.of(
            "今晚轻轻的：TA 只剩两格电，少讲道理多盖被子 🕯️",
            "今晚轻轻的：TA 想被温柔对待，进门先抱一下再说别的 🫂",
            "今晚轻轻的：TA 的灯快没油了，先递热水别提问 🫖",
            "今晚轻轻的：今天 TA 不好撑，让她靠一会儿就好 🛏️");
}
