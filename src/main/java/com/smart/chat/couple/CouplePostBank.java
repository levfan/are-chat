package com.smart.chat.couple;

import java.util.List;

/**
 * 明日邮局内容库（批次二十五 F290-F299）。静态内容只增不改顺序。
 */
public final class CouplePostBank {

    private CouplePostBank() {
    }

    /** F295 许愿井 52 周题卷（按年内周序轮换）。 */
    public static String wellQuestion(int weekOfYear) {
        return WELL_QUESTIONS.get(Math.floorMod(weekOfYear - 1, WELL_QUESTIONS.size()));
    }

    private static final List<String> WELL_QUESTIONS = List.of(
            "如果我们无所不能，这周先去做什么？",
            "不用考虑钱，这周我们最该去干嘛？",
            "如果时间多出一天，这周拿来干什么？",
            "如果 TA 会魔法，你希望这周 TA 变出什么？",
            "没有任何理由也会开心的一件事，这周选哪个？",
            "如果只能再排一个「人生清单」，这周加什么？",
            "这周想把哪个「改天一定」变成「就今天」？",
            "如果梦能预售，这周先兑哪一个？",
            "想让十年后的我们感谢这周的什么决定？",
            "如果这周可以「翘班去约会」，去哪？",
            "给未来的家再添一件什么小东西？",
            "这周为 50 岁的我们存一句什么话？");

    /** F299 额度档位梗（按履约净额）。 */
    public static String creditTier(long score) {
        if (score >= 12) {
            return "未来银行 VIP：说出口的事基本等于已经发生 🏦";
        }
        if (score >= 6) {
            return "优质信用：承诺兑现率跑赢多数人 💳";
        }
        if (score >= 3) {
            return "小额授信：可以开始立大一点的旗了 📈";
        }
        if (score >= 1) {
            return "信用起步：先从「顺手的事」攒起 🌱";
        }
        return "口头额度：建议少立旗、多圆旗 🕳️";
    }

    /** F296 接龙开场话术。 */
    public static final String RELAY_SEAL_LINE = "已封存给未来的 TA，到期自动出现在 TA 的邮筒里 ✉️";

    /** F290 新年卡放行话术。 */
    public static String oathDeliverLine(String year) {
        return year + " 年写下的新年卡到期了，请查收五年后的回信 💌";
    }
}
