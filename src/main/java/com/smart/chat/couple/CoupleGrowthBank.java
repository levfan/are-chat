package com.smart.chat.couple;

import java.util.List;
import java.util.Map;

/**
 * 共同养成内容库（F70/F77）：
 * 每日挑战题库、存款鼓励语、星座配对静态数据（元素相性 + 稳定指数 + 评语模板）。
 * 全部为内置静态内容，只增不改既有条目顺序。
 */
public final class CoupleGrowthBank {

    private CoupleGrowthBank() {
    }

    // ========== F70 每日挑战题库（40 条，按空间+天稳定一题） ==========

    private static final List<String> CHALLENGE_TASKS = List.of(
            "今天夸对方 3 次，要夸到具体的点",
            "给对方发一张现在的自拍（不许修图）",
            "说一件今天没告诉过 TA 的小事",
            "用 TA 喜欢的称呼叫 TA 一整天",
            "今天由你先说晚安",
            "给对方点一杯 TA 爱喝的（或记下来下次补）",
            "发一条 60 秒语音，讲讲你现在的状态",
            "问 TA「今天最想被怎样对待」，然后照做",
            "夸一个 TA 最近做得好的决定",
            "今天不准说「随便」，有想法直说",
            "给 TA 讲一个你小时候的故事",
            "发一个 TA 没见过的你的表情包",
            "今天主动承担一件 TA 本来要做的事",
            "给 TA 一句只有你俩懂的暗号",
            "认真问一句「你最近累吗」，等完整回答",
            "把今天的一件开心事归功于 TA",
            "发一首「这很像你」的歌给 TA",
            "今天睡觉前想三个 TA 的优点",
            "陪 TA 做一件 TA 想做但没说的事",
            "用三个词形容今天的 TA，并发给 TA",
            "给 TA 的朋友圈/动态认真写条评论",
            "今天把「谢谢」和「辛苦了」都说出口",
            "发一张你觉得「TA 会喜欢」的图",
            "问 TA「下周想吃什么」，记下来",
            "今天对 TA 说一句你从没说过的情话",
            "主动汇报你今天的三件小事",
            "给 TA 一个不少于 10 秒的（云）拥抱",
            "今天不催 TA 任何事",
            "问 TA「有什么我能帮你分担的」",
            "发一张你们的合照，配一句今天的心情",
            "今天让 TA 决定一件你俩的小事",
            "夸 TA 今天的样子（细节一点）",
            "说一个你想跟 TA 一起完成的新愿望",
            "今天睡前互相说一件明天期待的事",
            "模仿 TA 说话的语气发一条消息",
            "今天先承认自己做得不好的那件小事",
            "给 TA 讲个冷笑话（烂的也算）",
            "问 TA「最近有什么压力」，认真听",
            "发一张今天路过的风景",
            "对 TA 说「有你在真好」，别加表情包"
    );

    // ========== F71 存款鼓励语（按连续天数里程碑返回） ==========

    private static final Map<Long, String> PASSBOOK_MILESTONES = Map.of(
            3L, "💰 连续存款 3 天！利息是双倍的甜",
            7L, "🏆 连续存款一周！这段感情稳中有甜",
            21L, "🌟 连续存款 21 天！习惯成自然，爱也是",
            66L, "🧨 连续存款 66 天！六六大顺的两个人",
            100L, "👑 连续存款 100 天！你们是恋爱存折的传奇"
    );

    // ========== F77 星座配对（12 星座：key / 名称 / 元素） ==========

    private static final Object[][] ZODIACS = {
            {"aries", "白羊座 ♈", "fire"},
            {"taurus", "金牛座 ♉", "earth"},
            {"gemini", "双子座 ♊", "air"},
            {"cancer", "巨蟹座 ♋", "water"},
            {"leo", "狮子座 ♌", "fire"},
            {"virgo", "处女座 ♍", "earth"},
            {"libra", "天秤座 ♎", "air"},
            {"scorpio", "天蝎座 ♏", "water"},
            {"sagittarius", "射手座 ♐", "fire"},
            {"capricorn", "摩羯座 ♑", "earth"},
            {"aquarius", "水瓶座 ♒", "air"},
            {"pisces", "双鱼座 ♓", "water"}
    };

    private static final List<String> ZODIAC_COMMENTS = List.of(
            "你们的相处像老朋友，聊天不用打草稿",
            "一个负责冲，一个负责稳，刚好互补",
            "默契不靠 explanation，靠一个眼神",
            "你们吵架快和好更快，谁也记仇不过夜",
            "灵魂同频程度爆表，建议结伴走世界",
            "慢热但烧得久，是越处越甜的组合",
            "你们是彼此的情绪充电站",
            "一个爱说一个爱听，天然的内容生态闭环",
            "你们把日子过成了连续剧，而且天天想追",
            "互怼指数与恩爱指数一样高",
            "你们在一起的画面自带 BGM",
            "是那种别人看了想说「磕到了」的组合"
    );

    // ========== 读取与计算 ==========

    /** 今日挑战题目：按空间+天稳定一题。 */
    public static String pickChallenge(String spaceId, String day) {
        return CHALLENGE_TASKS.get(Math.floorMod(stableHash(spaceId + "|challenge|" + day), CHALLENGE_TASKS.size()));
    }

    /** 存款里程碑鼓励语（未达标返回 null）。 */
    public static String passbookMilestone(long streak) {
        return PASSBOOK_MILESTONES.get(streak);
    }

    /** 全部星座 key（保持顺序）。 */
    public static List<String> zodiacKeys() {
        return java.util.Arrays.stream(ZODIACS).map(r -> (String) r[0]).toList();
    }

    /** 星座显示名。 */
    public static String zodiacLabel(String key) {
        for (Object[] row : ZODIACS) {
            if (row[0].equals(key)) {
                return (String) row[1];
            }
        }
        return key;
    }

    private static String zodiacElement(String key) {
        for (Object[] row : ZODIACS) {
            if (row[0].equals(key)) {
                return (String) row[2];
            }
        }
        return "unknown";
    }

    /**
     * 配对指数：元素相同 95 分档；同系相生（火-风、土-水）90 分档；
     * 其余按双方 key 的稳定哈希落在 70-89 区间。同天同对结果恒定。
     */
    public static CoupleGrowthService.ZodiacVO zodiacPair(String mine, String partner) {
        String a = zodiacElement(mine);
        String b = zodiacElement(partner);
        int score;
        if (a.equals(b)) {
            score = 95;
        } else if ((a.equals("fire") && b.equals("air")) || (a.equals("air") && b.equals("fire"))
                || (a.equals("earth") && b.equals("water")) || (a.equals("water") && b.equals("earth"))) {
            score = 90;
        } else {
            int hash = Math.floorMod(stableHash(mine + "+" + partner), 20);
            score = 70 + hash;
        }
        String comment = ZODIAC_COMMENTS.get(Math.floorMod(stableHash(mine + "~" + partner), ZODIAC_COMMENTS.size()));
        return new CoupleGrowthService.ZodiacVO(mine, zodiacLabel(mine), partner, zodiacLabel(partner), score, comment);
    }

    /** 「下次一定」催办文案。 */
    public static String nudgeLine(String content) {
        return "⏰ 你随口说过的「下次一定」被 TA 翻牌啦：「" + content + "」——今天兑现，明天下架";
    }

    /** 稳定字符串哈希（FNV-1a 32 位），与 CoupleRitualBank 一致。 */
    public static int stableHash(String input) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < input.length(); i++) {
            hash ^= input.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }
}
