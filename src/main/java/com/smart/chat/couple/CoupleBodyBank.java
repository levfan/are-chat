package com.smart.chat.couple;

import java.util.List;

/**
 * 身体通知系统内容库（批次二十七 F310-F319）。静态内容只增不改顺序。
 * 口径：这里全部是陪伴话术，不做医疗建议、不下判断。
 */
public final class CoupleBodyBank {

    private CoupleBodyBank() {
    }

    /** F310 异常推给 TA 的提醒模板。 */
    public static String metricAlertLine(long seed, String who, String what) {
        return METRIC_ALERT.get(Math.floorMod(seed, METRIC_ALERT.size()))
                .replace("{u}", who).replace("{w}", what);
    }

    private static final List<String> METRIC_ALERT = List.of(
            "{u} 今天的{w}过了自己划的线，别讲道理，先问一句「现在要什么」。",
            "{u} 的{w}不太对， TA 大概不想被当病人，想被当女朋友/男朋友。",
            "{u} 报了{w}——此刻一杯热水的杀伤力大于一百句叮嘱。");

    /** F311 震感档位话术。 */
    public static String snoreLine(long seed, int level) {
        if (level <= 0) {
            return "今早自报没打呼——耳根清净，记一功。";
        }
        String line = SNORE_LINES.get(Math.min(level, SNORE_LINES.size()) - 1);
        return SNORE_HEAD.get(Math.floorMod(seed, SNORE_HEAD.size())) + line;
    }

    private static final List<String> SNORE_HEAD = List.of(
            "震感报告：", "昨夜现场：", "气象台播报：");

    private static final List<String> SNORE_LINES = List.of(
            "像远处有人在装修，能忍，偶尔抬头看一眼。",
            "间歇性电钻，被子已蒙过头，但梦被打断两次。",
            "整栋楼以为在放雷，建议明天单独睡一次给彼此一个假期。");

    /** F312 照顾卡（TA 递的「我能做」）。 */
    public static final List<String> CARE_CARDS = List.of(
            "热水袋 + 我抱着，30 分钟不打扰。",
            "今天家务我全包，你只负责躺着挑剧。",
            "冰的辣的我不给你递，想吃甜的我去买。",
            "不舒服就发消息，一个字都行，我秒回不算本事但能做到。");

    /** F312 阶段展示名。 */
    public static String phaseLabel(String phase) {
        return switch (phase == null ? "" : phase) {
            case "BEFORE" -> "提前预警";
            case "MENSTRUATING" -> "进行中";
            case "AFTER" -> "收尾期";
            case "OWULARE" -> "排卵期";
            default -> "没标";
        };
    }

    /** F313 破戒当天的陪绑话术。 */
    public static String cheerLine(long seed, int brokeCount) {
        return brokeCount <= 0
                ? BREAK_CLEAN.get(Math.floorMod(seed, BREAK_CLEAN.size()))
                : BREAK_BACK.get(Math.floorMod(seed, BREAK_BACK.size()));
    }

    private static final List<String> BREAK_CLEAN = List.of(
            "今天没破——数字小，但它是你自己攒的。",
            "干净的一天，营里给你记一笔。");

    private static final List<String> BREAK_BACK = List.of(
            "破戒不算塌方，把今天重新写上就行，营期不注销。",
            "第 N 次破戒也是第 N 次重新开始，我在这儿陪着。");

    /** F313 营期里程碑话术。 */
    public static String campMilestoneLine(int days) {
        return "营龄满 " + days + " 天：戒的东西少了 " + days + " 天，陪的人一天没缺席。";
    }

    /** F314 断链（30 分钟没接上）话术。 */
    public static String chainMissLine(long seed) {
        return CHAIN_MISS.get(Math.floorMod(seed, CHAIN_MISS.size()));
    }

    private static final List<String> CHAIN_MISS = List.of(
            "链子断了——半小时内没接上，明天早点开工。",
            "今日断链：一个人练也算练，但两个人一起才算链。");

    /** F315 「我能做」选项卡。 */
    public static final List<String> SOS_OPTIONS = List.of(
            "我去倒杯热水，坐在旁边不说话了。",
            "要吃药/要量体温的话我准备好，你说时间。",
            "今晚的活我接了，你什么都不用管。",
            "想被抱着还是想一个人待？我两个都能立刻切换。",
            "需要我打电话问问医院/买药吗？我只出力不追问。");

    /** F315 接住后的回执。 */
    public static String sosHeldLine() {
        return "接住了。不舒服不用道歉，也不用解释原因。";
    }

    /** F316 忌口标红话术。 */
    public static String redlineHitLine(String item, int hits) {
        return "今天饭票池里有 " + hits + " 票含「" + item + "」——红线项，做之前先撤了它。";
    }

    /** F317 虚拟陪同话术。 */
    public static String companionLine(long seed) {
        return COMPANION.get(Math.floorMod(seed, COMPANION.size()));
    }

    private static final List<String> COMPANION = List.of(
            "已到场（虚拟）：抽血那会儿我握着的是空气，但你那边赢了。",
            "陪同打卡：报告出来前我都在，出来以后我先看数字再看你。",
            "虚拟陪同就位：你在里面查，我在外面等，一步没走。");

    /** F318 情绪药友陪伴话术（非医嘱）。 */
    public static String medReplyLine(long seed, String how) {
        List<String> pool = switch (how == null ? "" : how) {
            case "HARD" -> MED_HARD;
            case "NONE" -> MED_NONE;
            default -> MED_STEADY;
        };
        return pool.get(Math.floorMod(seed, pool.size()));
    }

    private static final List<String> MED_STEADY = List.of(
            "稳着就好，不用报喜也不用报平安，我按周记。",
            "这周「还行」——我记下这两个字，下周继续问。");

    private static final List<String> MED_HARD = List.of(
            "这周难，我不劝你坚强，只问一句：现在需要我在吗？",
            "难受不用举例说明，你已经说了，就够了。");

    private static final List<String> MED_NONE = List.of(
            "这周没记也没关系，问一句本身就是我在意的方式。",
            "空着也行，下周我再来敲一次门。");

    /** F319 军令状违约话术。 */
    public static String oathBreachLine(long seed, int rate) {
        return rate == 0
                ? OATH_ZERO.get(Math.floorMod(seed, OATH_ZERO.size()))
                : "本周违约 " + rate + "%——军令状不罚钱，罚明天早十分钟关灯。";
    }

    private static final List<String> OATH_ZERO = List.of(
            "本周零违约：两条线都没破，这份自律可以拿去吹一年。");

    /** F319 双签生效话术。 */
    public static String oathSignedLine() {
        return "军令状双签生效：熄灯线写死了，谁破谁念给对方听。";
    }
}
