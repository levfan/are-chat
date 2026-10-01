package com.smart.chat.couple;

import java.util.List;

/**
 * 趣味游戏内容库（F130-F139，批次九静态库，只增不改顺序）：
 * 一百问题库、塔罗牌、恋爱天气、世界情话课、心动概率文案。
 * 抽取用 {@link CoupleRitualBank#stableHash}（FNV-1a）保证按 space+day 稳定。
 */
public final class CouplePlayBank {

    private CouplePlayBank() {
    }

    // ========== F130 一百问题库（100 题，只增不改） ==========

    private static final List<String> SURVEY_QUESTIONS = List.of(
            "你最喜欢我哪一点？", "你的原生家庭是什么样的？", "你最怕失去什么？", "你的梦想是什么？",
            "你小时候最喜欢玩什么？", "你人生中最难忘的一天？", "你最想学会的一项技能？", "你最欣赏的人是谁？",
            "你最近一次哭是因为什么？", "你最喜欢的电影？", "你最喜欢的书？", "你最喜欢的歌？",
            "你最喜欢的食物？", "你最讨厌的食物？", "你最喜欢的季节？", "你最喜欢的颜色？",
            "你最喜欢的城市？", "你最想去旅行的地方？", "你想住在海边还是山里？", "你喜欢猫还是狗？",
            "你的睡眠习惯是什么样的？", "你早起会先做什么？", "你睡前会想什么？", "你的解压方式是什么？",
            "你生气时会怎么办？", "你难过时会想一个人待着吗？", "你需要安慰时希望我怎么做？", "你希望我怎么哄你？",
            "你的底线是什么？", "你不能接受的缺点是什么？", "你最能包容我什么？", "你希望我改掉什么？",
            "你觉得爱是什么？", "你觉得好的感情是什么样的？", "你觉得我们最像哪种动物？", "你觉得我们像哪对荧幕情侣？",
            "你第一次见我时在想什么？", "你什么时候开始喜欢我的？", "我们的第一次约会你记得吗？", "我们之间最难忘的瞬间？",
            "你最喜欢我们一起做的什么事？", "你想和我一起做什么没做过的事？", "你想去哪个国家度蜜月？", "你想办一场什么样的婚礼？",
            "你想养宠物吗？叫什么名字？", "你想给孩子起什么名字？", "你想让我记住你的哪些习惯？", "你想让我在你生日做什么？",
            "你的健康小毛病有哪些？", "你的过敏源是什么？", "你的手机密码是什么意思？", "你的存钱目标是什么？",
            "你的职业理想是什么？", "你想退休后做什么？", "你希望六十岁的我们在干什么？", "你想学会的乐器？",
            "你会做饭吗？拿手菜是什么？", "你想让我学做的菜？", "你喜欢去菜市场吗？", "你喜欢的家居风格？",
            "你喜欢早起还是熬夜？", "你喜欢的周末怎么过？", "你喜欢热闹还是安静？", "你喜欢计划还是随性？",
            "你的社交电池能撑多久？", "你需要独处时间吗？", "你喜欢惊喜吗？", "你收过最好的礼物是什么？",
            "你希望收到什么礼物？", "你觉得仪式感重要吗？", "你喜欢写信还是发消息？", "你喜欢语音还是视频？",
            "你的交友观是什么？", "你的金钱观是什么？", "你的消费观是什么？", "你的教育观是什么？",
            "你最感激谁？", "你最遗憾的事？", "你想和解的过去是什么？", "你想告别什么？",
            "你的优点是什么？", "你的缺点是什么？", "别人对你的误解是什么？", "你最想被理解的是什么？",
            "你最近的压力来源？", "你最近的小确幸？", "你最近学到的新东西？", "你最近在追的剧？",
            "你手机里最舍不得删的照片？", "你的歌单里循环最多的一首？", "你最近想去吃的餐厅？", "你最近想看的海？",
            "如果我们吵架了你会先低头吗？", "你能接受冷战多久？", "你想让我怎么表达爱你？", "你最爱听的一句情话？",
            "你对我们的未来最期待什么？", "你觉得十年后的我们？", "你想对我说的最后一句话是什么？", "爱我现在，此刻你在想什么？"
    );

    // ========== F133 塔罗（22 张大阿尔卡那） ==========

    public record TarotCard(String name, String emoji, String message) {
    }

    private static final List<TarotCard> TAROT = List.of(
            new TarotCard("愚者", "🃏", "爱是未知的冒险，今天的你们适合说走就走。"),
            new TarotCard("魔术师", "🎩", "你们有把日常变成魔法的能力，今晚做点小惊喜吧。"),
            new TarotCard("女祭司", "🌙", "有些话不用说出口，你们的默契正在暗中生长。"),
            new TarotCard("皇后", "👑", "适合宠爱彼此的一天，把对方照顾得像个孩子。"),
            new TarotCard("皇帝", "🏛️", "稳定压倒一切，今天适合聊聊两个人的计划。"),
            new TarotCard("教皇", "📜", "找你们的「老规矩」做点什么，传统的仪式最安心。"),
            new TarotCard("恋人", "💞", "大吉！今天的空气都是粉色的，说什么都算情话。"),
            new TarotCard("战车", "🐎", "向着共同目标冲刺吧，你们是彼此最好的队友。"),
            new TarotCard("力量", "🦁", "用温柔化解最近的小别扭，软话比道理有用。"),
            new TarotCard("隐士", "🕯️", "给彼此留一点独处的空间，想念正在安静发酵。"),
            new TarotCard("命运之轮", "🎡", "缘分正在转动，今天的相遇会有特别的意义。"),
            new TarotCard("正义", "⚖️", "有分歧就摊开说，公平坦诚是你们最好的默契。"),
            new TarotCard("倒吊人", "🙃", "换个角度看问题，TA 的「无理取闹」可能有原因。"),
            new TarotCard("死神", "🍂", "该结束的坏习惯就让它结束，新的相处方式正在萌芽。"),
            new TarotCard("节制", "🍷", "细水长流的一天，慢慢说话，慢慢吃饭，慢慢相爱。"),
            new TarotCard("恶魔", "😈", "小心「上头式撒娇」，甜要适量，真实更要紧。"),
            new TarotCard("高塔", "🗼", "最近的小摩擦是提醒：别把情绪憋成塔，聊开就好。"),
            new TarotCard("星星", "⭐", "许愿的好日子，把对彼此的期待说出来。"),
            new TarotCard("月亮", "🌕", "有点小敏感的一天，多确认一句「我爱你就够了」。"),
            new TarotCard("太阳", "☀️", "大晴天！你们的关系正在被阳光晒得暖洋洋。"),
            new TarotCard("审判", "📣", "该做决定了：那件拖了很久的事，今天聊聊吧。"),
            new TarotCard("世界", "🌍", "圆满！你们就是把平凡日子过成全世界的那种人。")
    );

    // ========== F137 恋爱天气（5 种天气 + 提示） ==========

    public record LoveWeather(String name, String emoji, String tip) {
    }

    private static final List<LoveWeather> WEATHERS = List.of(
            new LoveWeather("晴", "☀️", "万里无云，适合说废话式情话。"),
            new LoveWeather("多云", "⛅", "偶尔飘过小情绪，记得及时回应对方。"),
            new LoveWeather("小雨", "🌧️", "有点小敏感，适合抱抱和软话。"),
            new LoveWeather("雷阵雨", "⛈️", "小心雷区话题，说话前先数三秒。"),
            new LoveWeather("彩虹", "🌈", "刚和好或刚互相表达过爱，甜度爆表。")
    );

    // ========== F134 世界情话课（每课：语言 / 原文 / 含义） ==========

    public record LoveLesson(String language, String word, String meaning) {
    }

    private static final List<LoveLesson> LESSONS = List.of(
            new LoveLesson("法语", "Tu es mon unique.", "你是我的唯一。"),
            new LoveLesson("日语", "君のことが好きだ。", "我喜欢你。"),
            new LoveLesson("韩语", "너 없인 안 돼.", "没有你不行。"),
            new LoveLesson("西班牙语", "Eres mi media naranja.", "你是我的另一半橙子（命中注定）。"),
            new LoveLesson("意大利语", "Ti vorrei bene per sempre.", "我想永远疼爱你。"),
            new LoveLesson("德语", "Du bist meine Ruhe.", "你是我的安宁。"),
            new LoveLesson("葡萄牙语", "Você é meu porto seguro.", "你是我的避风港。"),
            new LoveLesson("俄语", "Ты моё счастье.", "你是我的幸福。"),
            new LoveLesson("希腊语", "Είσαι το φως μου.", "你是我的光。"),
            new LoveLesson("泰语", "เธอคือความฝันที่เป็นจริง", "你是成真的梦。"),
            new LoveLesson("英语", "Home is wherever I'm with you.", "有你在的地方就是家。"),
            new LoveLesson("粤语", "我同你，行过千个秋天。", "我和你，走过千个秋天。"),
            new LoveLesson("文言", "既见君子，云胡不喜。", "见到你，怎能不欢喜。"),
            new LoveLesson("诗经", "执子之手，与子偕老。", "牵着你的手，陪你到老。"),
            new LoveLesson("挪威语", "Du er mitt liv.", "你是我的生命。"),
            new LoveLesson("阿拉伯语", "أنتِ عمري", "你就是我的生命。")
    );

    // ========== F132 心动概率文案（按指数分档） ==========

    private static final List<String> HEARTBEAT_LINES = List.of(
            "今天的心动刚刚好，像牵手时刚好合拍的脚步 💗",
            "电流有点强，见面时请保持一厘米以上的安全距离 ⚡",
            "心动指数爆表！今天的拥抱建议延长 30 秒 📈",
            "今天的怦然概率极高，看 TA 眼睛前请做好准备 🫀",
            "心跳同频，连打喷嚏都会一起 🫶"
    );

    public static List<String> surveyQuestions() {
        return SURVEY_QUESTIONS;
    }

    /** 一百问题库取第 n 题（1-based）。 */
    public static String surveyQuestion(int qNo) {
        if (qNo < 1 || qNo > SURVEY_QUESTIONS.size()) {
            return "题号超出范围啦";
        }
        return SURVEY_QUESTIONS.get(qNo - 1);
    }

    /** 塔罗：按 space+day 稳定抽一张。 */
    public static TarotCard pickTarot(String spaceId, String day) {
        return TAROT.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|tarot|" + day), TAROT.size()));
    }

    /** 恋爱天气：按 space+day 稳定预报。 */
    public static LoveWeather pickWeather(String spaceId, String day) {
        return WEATHERS.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|weather|" + day), WEATHERS.size()));
    }

    /** 世界情话课：按 space+day 稳定推一课。 */
    public static LoveLesson pickLesson(String spaceId, String day) {
        return LESSONS.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|lesson|" + day), LESSONS.size()));
    }

    /** 心动概率文案：0-100。 */
    public static String heartbeatLine(int score) {
        return HEARTBEAT_LINES.get(Math.min(HEARTBEAT_LINES.size() - 1, score * HEARTBEAT_LINES.size() / 100));
    }
}
