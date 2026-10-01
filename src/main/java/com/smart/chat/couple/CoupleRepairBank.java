package com.smart.chat.couple;

import java.util.List;

/**
 * 修复车间内容库（批次二十八 F320-F329）。静态内容只增不改顺序。
 * 口径：帮两个人把「吵完」变成「修好」，不评判谁对谁错，也不替谁说服谁。
 */
public final class CoupleRepairBank {

    private CoupleRepairBank() {
    }

    /** F320 解冻三问题干（slot 1-3）。 */
    public static String thawQuestion(int slot) {
        return slot <= 1 ? THAW_Q1 : slot == 2 ? THAW_Q2 : THAW_Q3;
    }

    private static final String THAW_Q1 = "这三问第一问：刚才那一刻，我其实怕的是什么？";
    private static final String THAW_Q2 = "第二问：我真正想要的，用一句话说是什么？";
    private static final String THAW_Q3 = "第三问：不等对方先动，我能先做的一件小事是？";

    /** F320 冷冻中提示（还没到点）。 */
    public static String freezeWaitLine(long minutes) {
        return "还在冷冻里，先等 TA 冷静完——剩余约 " + minutes + " 分钟，签了也不算数。";
    }

    /** F320 解冻成功话术。 */
    public static String thawedLine(long seed) {
        return THAWED.get(Math.floorMod(seed, THAWED.size()));
    }

    private static final List<String> THAWED = List.of(
            "解冻完成：这段架没有赢家，但你们俩都从里面出来了。",
            "化了。吵完还愿意回答那三问的人，是真的想继续。",
            "复温成功——下次还会吵，但你们已经知道怎么回来了。");

    /** F321 六要素名。 */
    public static String pointLabel(String p) {
        return switch (p == null ? "" : p) {
            case "FACT" -> "说清事实";
            case "FEEL" -> "说出感受";
            case "BLAME" -> "不甩锅";
            case "SORRY" -> "认自己的那部分";
            case "FIX" -> "给出改法";
            case "ASK" -> "说出你要的";
            default -> p == null ? "" : p;
        };
    }

    /** F321 验货结论话术。 */
    public static String verifyLine(boolean pass, long seed) {
        List<String> pool = pass ? PASSED : BACKED;
        return pool.get(Math.floorMod(seed, pool.size()));
    }

    private static final List<String> PASSED = List.of(
            "验货通过：这封道歉信进陈列室，改天吵架时可以拿出来当范本。",
            "通过了。肯把「我错在哪」写具体的人，值得被好好收着。");

    private static final List<String> BACKED = List.of(
            "退回重写：像「行了行了我错了」这种，谁验谁不合格。",
            "不合格：事实、感受、你要什么都还没写，回来补一段。");

    /** F322 重来卡话术。 */
    public static String redoLine(long seed) {
        return REDO.get(Math.floorMod(seed, REDO.size()));
    }

    private static final List<String> REDO = List.of(
            "重来卡生效：同一句话再说一遍，这次你们都有麦克风和耳机。",
            "重放开始——这次的规则是：不许打断，每人 60 秒。");

    /** F324 台阶卡（到点自动递）。 */
    public static String stepCard(long seed) {
        return STEP_CARDS.get(Math.floorMod(seed, STEP_CARDS.size()));
    }

    private static final List<String> STEP_CARDS = List.of(
            "台阶卡：去倒两杯水，一杯放到 TA 手边，不用说话。",
            "台阶卡：发一句「我还在生气，但我不想一个人待着」。",
            "台阶卡：把刚才那句话换个说法再说一次，只改语气不改意思。",
            "台阶卡：先承认一件小事——今天这件里属于你那 10%。",
            "台阶卡：什么也别讲，先抱 20 秒，计数我来。");

    /** F324 暂停话术。 */
    public static String pauseLine(boolean pause) {
        return pause ? "倒计时被按了暂停——谁按的谁负责在准备好的时候重新按下。"
                : "倒计时继续，这次别再加分钟了。";
    }

    /** F326 底线被踩说明模板。 */
    public static String breachLine(String text) {
        return "踩到底线「" + text + "」——红线记录要写清：当时为什么没刹住，下次怎么刹。";
    }

    /** F328 修复礼盒任务池。 */
    public static String boxTask(long seed) {
        return BOX_TASKS.get(Math.floorMod(seed, BOX_TASKS.size()));
    }

    private static final List<String> BOX_TASKS = List.of(
            "写三件 TA 最近为你做过的、你没道过谢的小事。",
            "把这次吵架里你先说的那句狠话，改成一句软话发过去。",
            "安排一次十分钟的散步，路上不谈这次的事。",
            "给对方做一件 TA 提过两次以上但你没做的小事。",
            "把和好的这天记下来，明年今天回看一次。");

    /** F328 礼盒完成话术。 */
    public static String boxDoneLine(long seed) {
        return BOX_DONE.get(Math.floorMod(seed, BOX_DONE.size()));
    }

    private static final List<String> BOX_DONE = List.of(
            "礼盒拆完：补偿不是还债，是你们把这段的疤换成了记号。",
            "任务完成——修好的证据又多了一条。");

    /** F327 最感人认错话术。 */
    public static String touchedLine() {
        return "被标为「最感人的一次认错」：这句以后可以当模板用。";
    }

    /** F329 纪念碑话术。 */
    public static String peaceLine(long seed) {
        return PEACE.get(Math.floorMod(seed, PEACE.size()));
    }

    private static final List<String> PEACE = List.of(
            "刻碑：这句话当时最伤人，现在最像证据——你们已经跨过它了。",
            "归档：留着这句，是为了以后确认自己真的走到这儿了。");

    /** F325 年度和解奖称号。 */
    public static String reportPrize(long seed, int admits, int passes) {
        if (admits == 0 && passes == 0) {
            return "本届空缺奖：今年没修过，说明也没吵透——不算坏事，但下次记得来车间。";
        }
        String[] pool = passes >= 3
                ? new String[]{"最佳修复奖", "金牌和解人", "陈列室常客奖"}
                : new String[]{"肯先低头奖", "台阶递得最快奖", "最会重写道歉奖"};
        return pool[Math.floorMod(seed, pool.length)];
    }

    /** F325 年报收尾话术。 */
    public static String reportSummary(long seed, int quarrels, int thawed) {
        return quarrels == 0
                ? "这一年车间没开张：要么真顺，要么有几次没记录下来——我猜是后者。"
                : "这一年挂过 " + quarrels + " 次冷冻，化了 " + thawed + " 次。修好这件事，你们已经练成手艺了。"
                        + EXTRA.get(Math.floorMod(seed, EXTRA.size()));
    }

    private static final List<String> EXTRA = List.of(
            "", "顺便说一句：回来的人比不吵的人更难得。", "下次吵架前先想想这三问。");
}
