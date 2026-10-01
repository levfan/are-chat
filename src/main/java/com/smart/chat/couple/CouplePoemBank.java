package com.smart.chat.couple;

import java.util.List;

/**
 * 文字浪漫内容库（F165/F167/F168/F169 静态库，只增不改顺序）：
 * 灵魂提问、恋爱语录模板、情书模板、手账贴纸。抽取用 {@link CoupleRitualBank#stableHash}（FNV-1a）。
 */
public final class CouplePoemBank {

    private CouplePoemBank() {
    }

    // ========== F165 灵魂提问盲盒（24 问） ==========

    private static final List<String> SOUL_QUESTIONS = List.of(
            "如果明天世界暂停一天，你最想和我做哪件事？",
            "你人生里最想重来的一刻是什么？我在旁边会怎么帮你？",
            "我们的爱情如果是一部电影，片名会是什么？",
            "你小时候最怕什么？现在还怕吗？",
            "哪一件事让你确定「就是这个人了」？",
            "如果能预知未来一年，你想先知道哪件事？",
            "你希望 80 岁的我们，还在做哪件现在做的事？",
            "你最近一次偷偷感动，是因为什么小事？",
            "如果给我们俩的性格各换一个超能力，你选什么？",
            "你最想删掉的记忆是哪段？讲给我听，我陪你消化。",
            "你希望我以后多做什么？少做什么？",
            "如果给十年后的你写一句话，你会写什么？",
            "你觉得我们最像哪对（真实的或虚构的）couple？",
            "你心里有没有一个一直没说出口的小梦想？",
            "你疲惫的时候，最想要什么方式的安慰？",
            "我们吵过的哪次架，现在想起来你觉得最好笑？",
            "如果你能给我改一个习惯，你会改哪个？",
            "你最想和我去哪个没人认识我们的地方？",
            "你觉得我们这一年最大的变化是什么？",
            "你希望我们的家有一面什么墙？",
            "如果给我们的爱情定一个颜色，你选什么？",
            "你最想收到什么样的礼物？不是贵的，是那种「怎么知道我想要这个」的。",
            "你担心过我们的未来吗？现在还担心吗？",
            "用三个词形容我们这一年的感情，你会选哪三个？"
    );

    // ========== F167 恋爱语录模板（占位 {days} 一起天数 {partner} 对方） ==========

    private static final List<String> QUOTE_TEMPLATES = List.of(
            "我们在一起的 第{days} 天，依然想对你一见钟情。",
            "世界很大，我的世界只有两条路：一条通向你，一条通向 {partner} 的路上。",
            "第{days} 天了，喜欢你这件事，我一天都没旷工。",
            "别人数羊入睡，我数和你有关的第{days} 天。",
            "亲爱的 {partner}：日历越撕越薄，我们越写越厚——今天已经是第{days} 天。",
            "认识你之后，连时间都有了形状：它长成第{days} 天的样子。",
            "第{days} 天，我依然会因为你的一句晚安，把不开心调成静音。",
            "如果要给我的 {days} 天心动找一个词，那大概是：停不下来。"
    );

    // ========== F168 情书模板（8 封，{partner} 为对方称呼占位） ==========

    public record LetterTemplate(String title, String scene, String body) {
    }

    private static final List<LetterTemplate> LETTER_TEMPLATES = List.of(
            new LetterTemplate("雨天专属·想你的雨天", "下雨天",
                    "亲爱的 {partner}：\n外面下雨了，我第一个想到的是你。\n雨声很吵，但我心里很安静，因为装满了你。\n等天晴了，我们去踩水洼吧。"),
            new LetterTemplate("深夜加班·心疼牌", "TA 加班",
                    "亲爱的 {partner}：\n看到你还在忙，很心疼又很骄傲。\n你认真生活的样子，是我见过最好的风景。\n别忘了喝水，我在家等你。"),
            new LetterTemplate("吵架和好·台阶牌", "想和好",
                    "亲爱的 {partner}：\n白天的事我想了很久，气早就没了，只剩下舍不得。\n我们和好吧，我的世界有你才完整。\n先低头的人不是输了，是更爱了。"),
            new LetterTemplate("纪念日专属·时光牌", "纪念日",
                    "亲爱的 {partner}：\n又是一年，谢谢你把平凡的日子过成了纪念。\n往后的每一年，我都想这样和你一起数。\n你是我最好的决定。"),
            new LetterTemplate("异地专属·月光牌", "异地恋",
                    "亲爱的 {partner}：\n今晚的月亮很好看，可惜你不在旁边。\n但一想到我们看的是同一个月亮，就觉得不远。\n攒够想念，见面时一次还给你。"),
            new LetterTemplate("道歉专用·真诚牌", "我错了",
                    "亲爱的 {partner}：\n对不起，这次是我没做好。\n你说得对的地方我都记下了，我会改。\n请再给我一次机会，我打算用余生来证明。"),
            new LetterTemplate("表白充电·心动牌", "平常日子",
                    "亲爱的 {partner}：\n没有什么特别的日子，就是突然很想你。\n喜欢这种事藏不住，捂住了嘴巴，也会从眼睛里跑出来。\n所以写了这封信。"),
            new LetterTemplate("深夜晚安·安眠牌", "睡前",
                    "亲爱的 {partner}：\n今天辛苦啦，把烦恼都交给我保管。\n早点睡，梦里也要记得我。\n明天醒来，第一条消息一定是我。")
    );

    // ========== F169 手账贴纸（16 枚） ==========

    private static final List<String> STICKERS = List.of(
            "✨", "🌈", "🌷", "🍰", "🌙", "☕", "🍀", "🎈",
            "🐱", "🌞", "🫧", "📷", "🎧", "🍥", "🧸", "💫"
    );

    public static List<String> soulQuestions() {
        return SOUL_QUESTIONS;
    }

    /** 今日灵魂一问：按 space+day 稳定。 */
    public static String pickSoulQuestion(String spaceId, String day) {
        return SOUL_QUESTIONS.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|soul|" + day), SOUL_QUESTIONS.size()));
    }

    public static List<String> quoteTemplates() {
        return QUOTE_TEMPLATES;
    }

    public static List<LetterTemplate> letterTemplates() {
        return LETTER_TEMPLATES;
    }

    public static List<String> stickers() {
        return STICKERS;
    }
}
