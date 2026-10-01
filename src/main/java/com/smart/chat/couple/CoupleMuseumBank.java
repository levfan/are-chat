package com.smart.chat.couple;

import java.util.List;

/** 时光博物馆静态内容库：银发情话（F193）、隐藏成就定义（F195）、首页问候模板（F198）。只增不改顺序。 */
public final class CoupleMuseumBank {

    private CoupleMuseumBank() {
    }

    /** 银发情话：「80 岁的我们」每日一句对白。 */
    public static final List<String> SILVER_LINES = List.of(
            "「老头子，你今天又忘了我做的红烧肉咸不咸。」「不咸，跟我老伴一样甜。」",
            "「老太太，轮椅我来推，你负责指挥去哪晒太阳。」",
            "「我们金婚那天说什么来着？」「说下辈子还早点遇到，别再让我等那么多年。」",
            "「你这记性，忘了什么都忘不了给我掖被角。」",
            "「等牙齿都掉光了，软的豆腐脑我喝一半，你喝一半。」",
            "「八十年了，你的口头禅我还没听烦。」「巧了，你的我也还没说腻。」",
            "「老夫老妻还有什么好秀的？」「秀的就是每天早上醒来第一眼还是你。」",
            "「你慢点走，我从来都没嫌你走得慢。」",
            "「年轻时候吵架摔的门，现在想起来都想笑。」「笑完记得把热牛奶喝了。」",
            "「我们都八十了，你还叫我什么？」「叫老伴啊，这辈子唯一的头衔。」",
            "「这张老照片里是谁呀？」「是你呀，我的整个年轻时代。」",
            "「要是我先走一步，你别怕，我下辈子还按老地方找你。」"
    );

    /** 按 space+day 稳定抽一条银发情话。 */
    public static String pickSilverLine(String spaceId, String day) {
        return SILVER_LINES.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|silver|" + day), SILVER_LINES.size()));
    }

    /** 隐藏成就定义：code 稳定不可改，threshold 为达标线。 */
    public record HiddenAchievement(String code, String name, String emoji, String desc, String source, int threshold) {
    }

    public static final List<HiddenAchievement> ACHIEVEMENTS = List.of(
            new HiddenAchievement("THANKS_10", "拾光感恩者", "🌾", "感恩便签攒满 10 条", "thanks", 10),
            new HiddenAchievement("FLASH_5", "心动收藏家", "⚡", "心动闪光记下 5 次", "flash", 5),
            new HiddenAchievement("JOURNAL_7", "手账连载人", "📔", "贴纸手账写满 7 页", "journal", 7),
            new HiddenAchievement("SIGNAL_3", "接头专家", "🤝", "动作暗语约定 3 条", "signal", 3),
            new HiddenAchievement("WHATIF_10", "脑洞共鸣体", "🌌", "「如果」问答双答满 10 天", "whatif", 10),
            new HiddenAchievement("SYNC_HIT_1", "同频电波", "🎧", "同频共振成功命中 1 次", "sync", 1)
    );

    /** 按 source 汇总出的计数是否达标。 */
    public static boolean reached(HiddenAchievement a, int count) {
        return count >= a.threshold();
    }

    /** 首页问候：按时段给一句开场白模板，{days} 为在一起天数。 */
    public static final String[][] GREETINGS = {
            {"清晨", "🌅", "早安，在一起的第 {days} 天，今天也先照顾彼此，再照顾世界。"},
            {"上午", "☕", "上午好，在一起的第 {days} 天，忙归忙，记得喝口水。"},
            {"午后", "🍃", "午后的风正好，在一起的第 {days} 天，偷五分钟想想 TA。"},
            {"傍晚", "🌇", "傍晚啦，在一起的第 {days} 天，今天的小事也值得说给 TA 听。"},
            {"夜晚", "🌙", "晚上好，在一起的第 {days} 天，别把明天的话留到今天吵架。"},
            {"深夜", "🌌", "夜深了，在一起的第 {days} 天，晚安比什么都重要。"}
    };

    /** 按小时取时段问候模板（0-23）。 */
    public static String[] greetingOf(int hour) {
        if (hour >= 5 && hour < 8) {
            return GREETINGS[0];
        }
        if (hour >= 8 && hour < 11) {
            return GREETINGS[1];
        }
        if (hour >= 11 && hour < 15) {
            return GREETINGS[2];
        }
        if (hour >= 15 && hour < 19) {
            return GREETINGS[3];
        }
        if (hour >= 19 && hour < 23) {
            return GREETINGS[4];
        }
        return GREETINGS[5];
    }
}
