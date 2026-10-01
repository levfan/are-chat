package com.smart.chat.couple;

import java.util.List;

/**
 * 默契亲密内容库（F170/F173 静态库，只增不改顺序）：
 * 五爱语档案与 12 题测评卷、「如果」脑洞题。抽取用 {@link CoupleRitualBank#stableHash}（FNV-1a）。
 */
public final class CoupleSparkBank {

    private CoupleSparkBank() {
    }

    // ========== F170 五爱语档案 ==========

    public record LangMeta(String code, String name, String emoji, String desc, String tip) {
    }

    public static final List<LangMeta> LANGS = List.of(
            new LangMeta(CoupleLoveLang.LANG_WORDS, "肯定的言语", "💬", "一句「你真棒」就能让 TA 满血复活。", "每天至少一句具体夸奖，别只说「还好」。"),
            new LangMeta(CoupleLoveLang.LANG_TIME, "用心陪伴", "⏰", "放下手机的整段时间，比十句晚安都珍贵。", "每周留一段「只属于我们」的时间，不看手机。"),
            new LangMeta(CoupleLoveLang.LANG_GIFTS, "接受礼物", "🎁", "礼物不在贵，在「你怎么知道我想要这个」。", "记住 TA 随口提过的小东西，突然送出。"),
            new LangMeta(CoupleLoveLang.LANG_SERVICE, "服务的行动", "🧾", "你洗碗的背影，就是 TA 的情话。", "主动分担一件 TA 常做的小事，别等被要求。"),
            new LangMeta(CoupleLoveLang.LANG_TOUCH, "身体的接触", "🫂", "一个拥抱能解决的，就别用道理。", "出门前回家后各一个 3 秒以上的拥抱。")
    );

    // ========== F170 爱语测评卷（12 题，每题二选一） ==========

    public record QuizOption(String text, String lang) {
    }

    public record QuizQuestion(String question, QuizOption optionA, QuizOption optionB) {
    }

    private static final List<QuizQuestion> QUIZ = List.of(
            new QuizQuestion("TA 做了对的事，你最想怎么回应？", new QuizOption("写一段话认真夸 TA", CoupleLoveLang.LANG_WORDS), new QuizOption("给 TA 做一顿好吃的", CoupleLoveLang.LANG_SERVICE)),
            new QuizQuestion("疲惫的一天结束后，什么最治愈你？", new QuizOption("一个长长的拥抱", CoupleLoveLang.LANG_TOUCH), new QuizOption("TA 陪你安静地坐着", CoupleLoveLang.LANG_TIME)),
            new QuizQuestion("哪种「被惦记」更让你心动？", new QuizOption("TA 送来你随口提过的小东西", CoupleLoveLang.LANG_GIFTS), new QuizOption("TA 说出你做过的具体好事", CoupleLoveLang.LANG_WORDS)),
            new QuizQuestion("你希望 TA 怎么表达「我想你」？", new QuizOption("突然出现在你面前", CoupleLoveLang.LANG_TIME), new QuizOption("帮你把一直拖着的事办了", CoupleLoveLang.LANG_SERVICE)),
            new QuizQuestion("约会时你更在意什么？", new QuizOption("TA 的注意力都在你身上", CoupleLoveLang.LANG_TIME), new QuizOption("牵手、靠肩这些小接触", CoupleLoveLang.LANG_TOUCH)),
            new QuizQuestion("收到什么会让你拍照发朋友圈？", new QuizOption("手写的小卡片", CoupleLoveLang.LANG_WORDS), new QuizOption("一束没有理由的花", CoupleLoveLang.LANG_GIFTS)),
            new QuizQuestion("生病的时候，什么让你最安心？", new QuizOption("TA 摸摸你的额头", CoupleLoveLang.LANG_TOUCH), new QuizOption("TA 忙前忙后倒水拿药", CoupleLoveLang.LANG_SERVICE)),
            new QuizQuestion("纪念日你最想收到？", new QuizOption("一封长长的信", CoupleLoveLang.LANG_WORDS), new QuizOption("一份用心准备的礼物", CoupleLoveLang.LANG_GIFTS)),
            new QuizQuestion("哪种「我们很配」的证据你更信？", new QuizOption("沉默也不尴尬的相处", CoupleLoveLang.LANG_TIME), new QuizOption("TA 不自觉帮你剥虾", CoupleLoveLang.LANG_SERVICE)),
            new QuizQuestion("看恐怖片时你想被怎样安慰？", new QuizOption("被紧紧握住手", CoupleLoveLang.LANG_TOUCH), new QuizOption("被夸「你刚才表现真勇敢」", CoupleLoveLang.LANG_WORDS)),
            new QuizQuestion("旅行回家你最希望？", new QuizOption("收到 TA 准备的接风小礼物", CoupleLoveLang.LANG_GIFTS), new QuizOption("TA 已经把家里收拾好了", CoupleLoveLang.LANG_SERVICE)),
            new QuizQuestion("哪种道歉方式最容易让你心软？", new QuizOption("一个不由分说的拥抱", CoupleLoveLang.LANG_TOUCH), new QuizOption("认真说一段心里话", CoupleLoveLang.LANG_WORDS))
    );

    // ========== F173 「如果」脑洞题（20 问） ==========

    private static final List<String> WHAT_IFS = List.of(
            "如果中了五百万，我们第一笔钱花在哪？",
            "如果可以养任何一只动物，你养什么？我反对也没用那种。",
            "如果给我们放一年假，你想怎么过？",
            "如果交换人生一天，你最想体验我的哪件事？",
            "如果我们的家在山顶/海边/森林，你选哪个？",
            "如果只能带三样东西去无人岛，你带什么？",
            "如果可以学会一种瞬间技能，你选什么？",
            "如果我们有一个餐厅，菜单只有三道菜，是什么？",
            "如果穿越回我们第一次见面，你想对当时的自己说什么？",
            "如果世界上只剩一种甜点，你希望是什么？",
            "如果给你一次不做家务的特权，你想换掉哪件？",
            "如果我们的日子是一部情景喜剧，第一集演什么？",
            "如果可以把一个周末变成 72 小时，我们怎么用？",
            "如果你能给我们的未来加一个滤镜，你加什么色调？",
            "如果开一家小店，你营业、我掌柜，卖什么？",
            "如果你能和 60 岁的我们通话 1 分钟，你会问什么？",
            "如果世界上所有音乐只能留一首，你选哪首？",
            "如果我们可以拥有一个超能力，但必须共用，你选什么？",
            "如果给我们的下一年起一个主题，你叫它什么年？",
            "如果现在立刻出发去一个地方，你说，我收拾行李。"
    );

    public static List<LangMeta> langs() {
        return LANGS;
    }

    public static List<QuizQuestion> quiz() {
        return QUIZ;
    }

    public static List<String> whatIfs() {
        return WHAT_IFS;
    }

    /** 今日「如果」一题：按 space+day 稳定。 */
    public static String pickWhatIf(String spaceId, String day) {
        return WHAT_IFS.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|whatif|" + day), WHAT_IFS.size()));
    }

    /** 按代码取爱语档案。 */
    public static LangMeta langOf(String code) {
        return LANGS.stream().filter(l -> l.code().equals(code)).findFirst().orElse(LANGS.get(0));
    }
}
