package com.smart.chat.couple;

import java.util.List;

/** 饭桌系静态内容：点单机心情饮品（F216）与饭桌话题卡（F218），无表、只增不改。 */
public final class CoupleDiningBank {

    private CoupleDiningBank() {
    }

    /** 心情饮品：今天的心情配一杯。 */
    public record Drink(String mood, String emoji, String name, String note) {
    }

    public static final List<Drink> DRINKS = List.of(
            new Drink("开心", "🧋", "全糖珍珠奶茶", "开心就要甜上加甜！"),
            new Drink("有点累", "☕", "热可可燕麦拿铁", "累的时候喝点热的，暖一暖"),
            new Drink("emo了", "🍋", "气泡柠檬水", "酸一下，把emo顶回去"),
            new Drink("想庆祝", "🍾", "无醇起泡葡萄汁", "干杯！小事也值得庆祝"),
            new Drink("平平淡淡", "🍵", "温热的茉莉花茶", "淡一点的日子也很好")
    );

    /** 按心情找饮品，找不到返回第一杯。 */
    public static Drink drinkOf(String mood) {
        String m = mood == null ? "" : mood.trim();
        return DRINKS.stream().filter(d -> d.mood().equals(m)).findFirst().orElse(DRINKS.get(0));
    }

    /** 饭桌话题卡：边吃边聊的小问题。 */
    public static final List<String> TOPICS = List.of(
            "今天发生的最离谱的一件事是什么？",
            "如果明天不用上班，我们第一件事去干嘛？",
            "最近有什么东西是你一直想试但没敢试的？",
            "如果咱俩开一家小店，你希望是卖什么的？",
            "今天有没有哪个瞬间想我？",
            "你小时候最喜欢吃的一道菜是什么？",
            "如果能瞬间学会一项技能，你选什么？",
            "我们上一次一起笑到肚子疼是因为什么？",
            "最近手机里存得最多的一类东西是什么？",
            "如果去对方的城市生活一年，你最先不适应什么？",
            "你觉得我身上最像小孩的地方是哪里？",
            "如果中了一百万，我们先花掉的第一笔是什么？",
            "最近有什么让你有点焦虑的事？",
            "你记忆里最好吃的一顿饭是在哪儿吃的？",
            "如果我们可以养一只会说话的宠物，你选什么？",
            "你想对我做但一直没开口的一件小事是什么？",
            "如果给今天的心情打个菜名，会是什么菜？",
            "你最近偷偷坚持的一件小事是什么？",
            "如果重来一次，你想重回哪个夏天？",
            "你觉得我们俩最不像的一对的地方是什么？",
            "有什么歌是你不好意思承认很喜欢听的？",
            "如果今晚可以点任何一家店的菜，你点谁？",
            "你最近梦到过什么奇怪的家伙？",
            "如果给我们的饭桌立一条新规矩，你立什么？"
    );

    /** 按 space+day 稳定抽一条今日话题（同一天双方看到同一题）。 */
    public static String topicOf(String spaceId, String day) {
        int idx = Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|dine-topic|" + day), TOPICS.size());
        return TOPICS.get(idx);
    }
}
