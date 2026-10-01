package com.smart.chat.couple;

import java.util.List;

/**
 * 异地恋内容库（F110-F119，批次七静态库，只增不改顺序）：
 * 云约会灵感清单、见面能量瓶文案、牵手/想念里程碑文案。
 * 抽取用 {@link CoupleRitualBank#stableHash}（FNV-1a）保证稳定。
 */
public final class CoupleDistanceBank {

    private CoupleDistanceBank() {
    }

    // ========== F116 云约会灵感（异地也能一起做的事） ==========

    private static final List<String> CLOUD_DATES = List.of(
            "连麦看同一部电影，同步按下播放键",
            "视频里一起吃晚饭，比比谁的卖相好",
            "同时点开同一首歌，安静听完整首",
            "连麦一起打游戏，双排上分",
            "一起看日出/日落：各自拍下当时的天空",
            "给对方读一段书/一篇小文章",
            "同步逛超市：视频里帮对方挑水果",
            "连线一起做饭，做同一道菜",
            "同时下单给对方的惊喜快递",
            "连麦自习/加班，互相监督不打扰",
            "一起看月亮：分享各自窗外的月色",
            "视频逛博物馆/云展览，边看边聊",
            "约定同一时刻抬头看云，描述给对方",
            "连麦拼图/你画我猜小游戏",
            "互寄一封手写信，同时寄出",
            "同步点同一家的外卖，云聚餐",
            "一起看老照片，各自讲当天的故事",
            "视频陪对方散步一圈",
            "连麦看同一集综艺，同步哈哈哈",
            "睡前连麦读书，读到谁先睡着"
    );

    // ========== F113 见面能量瓶文案（30 天一满） ==========

    /** 能量等级文案：0-24 刚充能 / 25-49 想念升温 / 50-79 火急火燎 / 80-100 满格待见面。 */
    private static final List<String> ENERGY_LINES = List.of(
            "能量刚充上，思念开始发芽 🌱",
            "想念在升温，记得多发语音呀 📶",
            "能量过半，车票该看看啦 🚄",
            "快满了！见面的日子定了吗 🥺",
            "能量满格！这瓶思念只能见面才能放下 💯"
    );

    public static List<String> cloudDateSuggestions() {
        return CLOUD_DATES;
    }

    /** 云约会灵感：按空间+序号稳定取。 */
    public static String pickCloudDate(String spaceId, int index) {
        return CLOUD_DATES.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|cloud|" + index), CLOUD_DATES.size()));
    }

    /** 能量文案：energy 0-100。 */
    public static String energyLine(int energy) {
        int level = Math.min(4, energy / 25);
        return ENERGY_LINES.get(level);
    }

    // ========== F110/F112 里程碑文案 ==========

    /** 牵手里程碑：5/10/30/50/100 天。 */
    public static String handholdMilestone(long days) {
        if (days >= 100) {
            return "💯 牵手满 100 天！你们的手早就长在一起了吧";
        }
        if (days >= 50) {
            return "🤝 牵手 50 天，隔着屏幕的体温都是热的";
        }
        if (days >= 30) {
            return "🌙 牵手满一个月，月亮都在替我们高兴";
        }
        if (days >= 10) {
            return "✨ 牵手 10 天，小手已经牵出默契";
        }
        if (days >= 5) {
            return "🖐️ 牵手 5 天，继续保持哦";
        }
        return null;
    }

    /** 双向奔赴里程碑：3/7/15/30/60 次。 */
    public static String missMilestone(long times) {
        if (times >= 60) {
            return "💞 双向奔赴 60 次！想念会转弯，最后都找到彼此";
        }
        if (times >= 30) {
            return "🫶 双向奔赴 30 次，你们把想念过成了日常";
        }
        if (times >= 15) {
            return "🌠 双向奔赴 15 次，同一片星空下的默契";
        }
        if (times >= 7) {
            return "💌 双向奔赴一周连击，想念都有回声";
        }
        if (times >= 3) {
            return "💗 已经 3 次双向奔赴啦";
        }
        return null;
    }
}
