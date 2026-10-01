package com.smart.chat.couple;

import java.util.List;

/**
 * 传世系统内容库（批次三十 F340-F349，v6 收官批）。静态内容只增不改顺序。
 * 口径：把一年攒下来的东西年度化、资产化——十问、年审、发布会、汇率、品牌、盘点、清单、抽奖、等级。
 */
public final class CoupleLegacyBank {

    private CoupleLegacyBank() {
    }

    /** F340 年度十问题面（固定顺序，跨年可比）。 */
    public static final List<String> TEN_QUESTIONS = List.of(
            "今年我们最好的一次是哪天？",
            "今年吵得最凶的那次，后来是怎么好的？",
            "今年我为你改变的一件小事是什么？",
            "今年我最想谢你的一件事是什么？",
            "今年我们新学会的一件事（菜/运动/技能）？",
            "今年我最想删掉的一段记忆是什么？",
            "今年你最让我意外的一次是什么？",
            "今年我们的钱花得最值的地方是？",
            "如果明年只能实现一个约定，我希望是？",
            "用一个词形容我们的今年，我会说：");

    /** F342 发布会评分卡档位。 */
    public static String speechScoreLine(int score) {
        return score >= 5 ? SPEECH_5 : score == 4 ? SPEECH_4 : score == 3 ? SPEECH_3
                : score == 2 ? SPEECH_2 : SPEECH_1;
    }

    private static final String SPEECH_5 = "满分发言：这段明年可以直接放进年度盘点里。";
    private static final String SPEECH_4 = "很高：内容具体，情绪也到了，扣一分怕你骄傲。";
    private static final String SPEECH_3 = "合格：说了事，但少了点「我具体要什么」。";
    private static final String SPEECH_2 = "偏虚：像年终总结，不像对我们说的话。";
    private static final String SPEECH_1 = "重写：这版我听完不知道该配合什么。";

    /** F343 里程碑加速建议池。 */
    public static String speedupLine(long seed) {
        return SPEEDUP.get(Math.floorMod(seed, SPEEDUP.size()));
    }

    private static final List<String> SPEEDUP = List.of(
            "提速建议：把打卡时间固定在同一件小事之后（比如晚饭后），顺手就不费力。",
            "提速建议：断一天别补两天，按原速走，节奏比数字重要。",
            "提速建议：每周留一天「双人都在线」，进度基本全靠那天。",
            "提速建议：把目标改成两人各占一半，别互相等。");

    /** F344 年末汇率结算梗。 */
    public static String fxSettleLine(long seed, int kissToHug, int hugToWord) {
        return "年末汇率结算：1 个亲亲 = " + kissToHug + " 个抱抱 = " + (kissToHug * hugToWord)
                + " 句夸夸。" + FX.get(Math.floorMod(seed, FX.size()));
    }

    private static final List<String> FX = List.of(
            "本年度通胀严重，夸夸成本最低，建议多印。",
            "汇率由两人共同维护，任何一方单方面调价无效。",
            "结算后不追溯，明年重新开盘。");

    /** F345 品牌发布话术。 */
    public static String brandLine(long seed) {
        return BRAND.get(Math.floorMod(seed, BRAND.size()));
    }

    private static final List<String> BRAND = List.of(
            "品牌已发布：以后这个空间的头部写着我们自己的名字。",
            "官宣完毕。别人的情侣空间是模板，我们的是商标。");

    /** F347 传世清单封存话术。 */
    public static String sealedLine(long seed) {
        return SEALED.get(Math.floorMod(seed, SEALED.size()));
    }

    private static final List<String> SEALED = List.of(
            "双签封存：这条不是现在用的，是留着以后救场的。",
            "封好了。清单上的东西越少越值钱，别乱加。");

    /** F348 奖池（迷你愿望位）。 */
    public static final List<String> PRIZES = List.of(
            "一次「你说什么都对」的两小时", "一顿你选的餐厅，另一人不许挑",
            "一部对方想看很久但一直没看的片", "一次无理由的外卖代点",
            "一个周末早上不用定闹钟的特权", "一次免做家务金牌",
            "一封手写信，内容随你", "一次地点盲选：他定你不问");

    /** F348 周年抽奖提醒话术。 */
    public static String drawRemindLine() {
        return "周年到了：抽奖箱已开封，两人各抽一次，奖券不许退。";
    }

    /** F349 空间等级称号（按互动总量门槛，索引即档位）。 */
    public static final List<String[]> LEVEL_TITLES = List.of(
            new String[]{"0", "刚开张的小铺"},
            new String[]{"50", "有常客了"},
            new String[]{"150", "街坊认识你们"},
            new String[]{"300", "小店成据点"},
            new String[]{"500", "开始有存货"},
            new String[]{"800", "两口子的小公司"},
            new String[]{"1200", "有分支了"},
            new String[]{"1800", "地方老字号"},
            new String[]{"2600", "上了年鉴"},
            new String[]{"3600", "可以传世了"});

    /** F349 等级话术。 */
    public static String levelLine(int level, String title) {
        return "空间等级 Lv." + level + "｜" + title + "——这是你们一起点出来的数，不是买的。";
    }

    /** F346 年度盘点收尾句。 */
    public static String reviewTail(long seed) {
        return REVIEW_TAIL.get(Math.floorMod(seed, REVIEW_TAIL.size()));
    }

    private static final List<String> REVIEW_TAIL = List.of(
            "明年不必更热闹，只要还愿意一起记。",
            "这份盘点没有模板句，数字全是你们自己攒的。",
            "存档完成，明年此时再打开看一眼。");
}
