package com.smart.chat.couple;

import java.util.List;

/**
 * 两家与朋友内容库（批次二十九 F330-F339）。静态内容只增不改顺序。
 * 口径：把「见家长、送礼、朋友怎么看我们」这些最容易慌的事，变成可以提前准备、可以商量的流程。
 */
public final class CoupleWorldBank {

    private CoupleWorldBank() {
    }

    /** F330 攻略建议池（{类别, 建议}，类别：BRING 带什么 / TALK 聊什么 / MINE 雷区）。 */
    public static final List<String[]> VISIT_TIPS = List.of(
            new String[]{"BRING", "先问清家里谁忌口，水果挑两样不踩雷的。"},
            new String[]{"BRING", "给长辈带能消耗的东西（茶/膏药/水果），比摆件好。"},
            new String[]{"TALK", "准备三个话题：TA 小时候、家里最近的事、长辈身体的近况。"},
            new String[]{"TALK", "多问少吹：让长辈讲他们的当年，你负责点头和追问。"},
            new String[]{"MINE", "别先接「工作稳不稳」这句，答完别反问。"},
            new String[]{"MINE", "婚期/买房/生娃三连问，提前商量好谁接哪一句。"},
            new String[]{"MINE", "不在饭桌上反驳长辈的养生观点，笑着点头就行。"});

    /** F330 战报开场。 */
    public static String visitReportLine(long seed) {
        return VISIT_REPORT.get(Math.floorMod(seed, VISIT_REPORT.size()));
    }

    private static final List<String> VISIT_REPORT = List.of(
            "战报写下来：哪句接住了、哪句差点翻车，下次就不用再赌。",
            "这一趟过去了。你俩互相救场的地方，值得记一笔。");

    /** F332 朋友视角三问题（按空间稳定取三题）。 */
    public static String friendQuestion(long seed, int slot) {
        int idx = Math.floorMod(seed, FRIEND_QUESTIONS.size());
        return FRIEND_QUESTIONS.get((idx + slot - 1) % FRIEND_QUESTIONS.size());
    }

    private static final List<String> FRIEND_QUESTIONS = List.of(
            "朋友眼里我们俩，谁更迁就谁？",
            "朋友觉得我们最像哪种组合（同学/老夫老妻/欢喜冤家）？",
            "如果朋友要给我们提一个建议，你猜会是什么？",
            "朋友说我们在一起之后，你身上最大的变化是什么？",
            "外面看我们，最容易看错的一点是什么？",
            "朋友觉得我们俩吵架一般几天就和好？");

    /** F332 他观卡话术。 */
    public static String friendViewLine(long seed) {
        return FRIEND_VIEW.get(Math.floorMod(seed, FRIEND_VIEW.size()));
    }

    private static final List<String> FRIEND_VIEW = List.of(
            "他观卡出炉：外人看到的我们，和自己感觉的总差半拍——差的那半拍挺可爱。",
            "朋友说的话不一定对，但值得抄下来留档。");

    /** F333 官宣月卡提示语。 */
    public static String declareLine(long seed) {
        return DECLARE.get(Math.floorMod(seed, DECLARE.size()));
    }

    private static final List<String> DECLARE = List.of(
            "本月官宣卡已发进编年：以后翻回来，能看到我们每个月是怎么说出口的。",
            "官宣这事不必一次到位，每个月各写一句，就是最真实的时间线。");

    /** F334 选稿话术。 */
    public static String captionPickedLine(long seed) {
        return CAPTION_PICKED.get(Math.floorMod(seed, CAPTION_PICKED.size()));
    }

    private static final List<String> CAPTION_PICKED = List.of(
            "定稿了。这条以后进百科，署名作者本人。",
            "选稿完成——TA 的候选也留着，下次有素材再战。");

    /** F335 陪同小包模板。 */
    public static final List<String> PACK_TEMPLATE = List.of(
            "充电线 + 充电宝", "常用药一小袋", "换洗袜子和一件外套",
            "纸巾湿巾", "雨伞", "TA 的身份证放包外层拉链袋");

    /** F336 亲戚称谓考前卷（{称谓问法, 标准答案}）。 */
    public static final List<String[]> RELATIVES_SAMPLE = List.of(
            new String[]{"你姑姑的女儿怎么称呼", "表姐或表妹"},
            new String[]{"你舅舅的妻子怎么称呼", "舅妈"},
            new String[]{"你妈妈的弟弟怎么称呼", "舅舅"},
            new String[]{"你姐姐的丈夫怎么称呼", "姐夫"},
            new String[]{"你堂哥的媳妇怎么称呼", "堂嫂"},
            new String[]{"你姨妈的儿子怎么称呼", "表兄或表弟"});

    /** F336 错题进考前强化的提示。 */
    public static String relativeWrongLine(String term) {
        return "「" + term + "」答错了，进考前强化清单——下次见面前务必背下。";
    }

    /** F337 社会信用话术。 */
    public static String vowWitnessLine(String content) {
        return "见证完成：「" + content + "」，到期没犯就自动解除。";
    }

    public static String vowKeptLine() {
        return "保证到期解除，社会信用+1：说到做到这件事，你们是真的在做。";
    }

    public static String vowBrokenLine(String note) {
        return "塌房记录生效——" + note + "。别删，留着当下次的前车。";
    }

    /** F338 群聊记者回执话术。 */
    public static String groupLaughLine(long seed, boolean both) {
        return both
                ? "两人都笑了：今天这素材双双通过，可称佳话。"
                : GROUP.get(Math.floorMod(seed, GROUP.size()));
    }

    private static final List<String> GROUP = List.of(
            "素材收到。群里最好笑的确实是我们，这个判断我很自信。",
            "记下来了，下次写进百科当词条。");

    /** F339 赔礼信模板。 */
    public static final List<String> APOLOGY_TEMPLATES = List.of(
            "那天我话说得太急，回去想了很久，是我不好。您别往心里去，也请给我一次改口的机会。",
            "这事我不该在气头上做。先给您赔个不是，后面怎么做我听您的意见。",
            "我知道让您为难了。道歉不是要您原谅，是想让您知道我把这事放在心上了。");

    /** F339 审阅话术。 */
    public static String apologyReviewLine(boolean pass) {
        return pass
                ? "信通过审阅：这封由 TA 替你递出去，体面留在了你们俩手里。"
                : "退回重写：口气太冲或者太软，都容易让误会变大。";
    }
}
