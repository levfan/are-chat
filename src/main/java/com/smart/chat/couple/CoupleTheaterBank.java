package com.smart.chat.couple;

import java.util.List;

/**
 * 扮演剧场内容库（批次二十六 F300-F309）。静态内容只增不改顺序。
 */
public final class CoupleTheaterBank {

    private CoupleTheaterBank() {
    }

    /** F300 今日身份池：{身份名, 与该身份相处指南}。 */
    public static final List<String[]> ROLE_POOL = List.of(
            new String[]{"今天刚上任的甲方", "需求一律说「再改一版」，但绝不告诉你要改哪里；顺着你就是过审。"},
            new String[]{"熬夜赶稿的编剧", "白天没精神，傍晚才活过来；夸 TA 的桥段比催 TA 睡觉管用。"},
            new String[]{"第一次带猫的兽医", "嘴上镇定，手上发抖；任何问题都可以问「你确定吗」。"},
            new String[]{"米其林后厨的副厨", "盐放多了会当场道歉，但不允许你说「随便吃点」；上菜要先报菜名。"},
            new String[]{"幼儿园最乖的小朋友", "被表扬会开心一整天；惹哭了要靠一颗糖和好，讲道理没用。"},
            new String[]{"退休返聘的老教授", "说话慢，爱讲「我们那时候」；打断 TA 会被记小本子。"},
            new String[]{"机场延误的旅客", "耐心余额为负；此时递上一杯水就是救命恩人。"},
            new String[]{"新来的健身房教练", "会劝你再多做一组；但 TA 自己先要有人鼓励。"},
            new String[]{"深夜便利店店员", "凌晨两点还在整理货架；对熟客的记忆好得吓人。"},
            new String[]{"第一次坐飞机的旅客", "全程抓紧扶手，落地第一句一定是「我们下次还坐吗」。"},
            new String[]{"被退货的手作陶艺师", "杯子摔了会笑着说「碎碎平安」，但心里疼一天。"},
            new String[]{"社区团购团长", "话痨、热心、爱统计；找 TA 办事要先夸 TA 组织得好。"},
            new String[]{"台风天值班的气象播报员", "播报时很稳，下班后最怕被问「明天到底下不下雨」。"},
            new String[]{"刚学会开手动挡的新司机", "起步熄火不丢人，被人笑才丢人；副驾只许夸，不许指导。"});

    /** F300 今日身份（按空间+日稳定抽取）。 */
    public static String[] roleOfToday(long seed) {
        return ROLE_POOL.get(Math.floorMod(seed, ROLE_POOL.size()));
    }

    /** F302 出师定级话术。 */
    public static String masterGradeLine(String grade) {
        return "GRADUATED".equals(grade)
                ? "师父点头了：这一届徒弟可以出师，往后家务你俩平摊。"
                : "师父留了级：手艺还差一点，下周继续侍奉，不许嫌烦。";
    }

    /** F303 电话亭接通话术（含「信号不好」彩蛋）。 */
    public static String boothStaticLine(long seed) {
        return STATIC_LINES.get(Math.floorMod(seed, STATIC_LINES.size()));
    }

    private static final List<String> STATIC_LINES = List.of(
            "滋滋……信号不好……你刚才那句再说一遍，我想听清楚。",
            "喂？喂？——跨了一年来电，杂音有点大，但你的声音一点没变。",
            "信号时好时坏，像极了那年我们吵架的夜晚；不过最后接通了。",
            "接通了。这边的我们过得很好，你留言里担心的事，后来没那么难。");

    /** F304 黑话抽查题干。 */
    public static String refQuizPrompt(String term) {
        return "抽查：我们的黑话「" + term + "」是什么意思？（别翻大全，凭记忆答）";
    }

    /** F304 谁先忘的判词。 */
    public static String refJudgeLine(boolean right) {
        return right
                ? "答对了——这个梗已经长进你脑子里，撕不掉。"
                : "答岔了——记到这儿为止，下次谁先忘谁请客。";
    }

    /** F305 每日奥斯卡颁奖词模板。 */
    public static String actAwardLine(long seed, String aboutUser) {
        return ACT_AWARD.get(Math.floorMod(seed, ACT_AWARD.size())).replace("{u}", aboutUser);
    }

    private static final List<String> ACT_AWARD = List.of(
            "今日最佳演技奖颁给 {u}：全场最像没事发生的那个人。",
            "{u} 提名到位：嘴上说「随便」，眼神已经写好了三个方案。",
            "评委会一致通过，{u} 演技稳如老狗，情绪价值片酬翻倍。",
            "{u} 这场戏细节拉满，建议直接进我们的百科留档。");

    /** F306 家长题池（如果我是你爸妈）。 */
    public static final List<String> FAMILY_QUESTIONS = List.of(
            "如果我是你爸妈，第一眼你会担心我挑你哪件毛病？",
            "如果我问你爸妈要「一年不许抱怨」的许可，你会替他们开什么条件？",
            "如果我是你爸妈，你觉得你最怕我撞见你们哪种相处场面？",
            "如果我坚持你们必须在家吃晚饭，你会砍掉哪一项安排来换？",
            "如果我是你爸妈，你想让我先夸你哪一点，好让我放心？",
            "如果我问「她/他到底哪里好」，你会举哪一件最小的事？",
            "如果我把你们约会的地方改成家里，你会加什么节目来救场？",
            "如果我要求你每天给我打一个电话，你会固定聊什么内容？",
            "如果我是你爸妈，你觉得我最可能误会你们的哪件事？",
            "如果我给你俩布置一门「家规作业」，你希望题目是什么？",
            "如果我说「先立业再谈恋爱」，你会怎么回我？",
            "如果我担心你被照顾不好，你会拿出哪条证据让我安心？",
            "如果我去你们的城市住一周，你最想安排哪一顿饭？",
            "如果我和你妈同时说你熬夜，你会先改哪个习惯？",
            "如果我觉得你花钱太大手大脚，你会怎么解释你的账？",
            "如果我问「吵架谁先低头」，你敢不敢把真实答案告诉我？",
            "如果我把结婚这事摆在饭桌上问，你希望你怎么替我挡？",
            "如果我想看看你们的聊天记录，你觉得哪一段最经得起看？",
            "如果我说「不合适就分吧」，你会用哪句话顶回来？",
            "如果我夸她/他比你懂事，你会不服气在哪一点？");

    /** F306 今日家长题（按空间+日稳定抽取）。 */
    public static String familyQuestionOfToday(long seed) {
        return FAMILY_QUESTIONS.get(Math.floorMod(seed, FAMILY_QUESTIONS.size()));
    }

    /** F307 剧终双视角剧本收尾话术。 */
    public static String movieScriptLine(long seed) {
        return SCRIPT_LINES.get(Math.floorMod(seed, SCRIPT_LINES.size()));
    }

    private static final List<String> SCRIPT_LINES = List.of(
            "两条角色线并排一放，才发现同一集你们各哭了一次。",
            "剧本合上了：同一部剧，两种追法，居然都指向对方。",
            "看完你们的角色日记，编剧该来取经。");

    /** F308 客服差评申诉判词。 */
    public static String appealLine(long seed) {
        return APPEAL_LINES.get(Math.floorMod(seed, APPEAL_LINES.size()));
    }

    private static final List<String> APPEAL_LINES = List.of(
            "客服申诉已受理：态度分可议，但心意不打折，明天重新上岗。",
            "申诉通过一半——下次响应再快些，差评就自动改成好评。",
            "申诉归档：这单确实委屈，但顾客的感受也记一大页。");

    /** F309 冷知识颁奖礼奖项名（按空间+日稳定抽取）。 */
    public static String galaPrize(long seed) {
        return GALA_PRIZES.get(Math.floorMod(seed, GALA_PRIZES.size()));
    }

    private static final List<String> GALA_PRIZES = List.of(
            "最佳忘词奖", "最佳抢戏奖", "最会接梗奖", "最佳配角意识流奖",
            "黑话十级证书", "最像一家人奖", "今日片场劳模奖", "最佳情绪价值奖");

    /** F309 颁奖礼收尾话术。 */
    public static String galaLine(long seed, int entries) {
        return entries <= 0
                ? "本届颁奖礼空缺——今天还没人上台表演，先去当一天别人。"
                : "本届共 " + entries + " 项提名在册，掌声由电话亭赞助播出。";
    }
}
