package com.smart.chat.couple;

import java.util.List;
import java.util.Map;

/**
 * 倾听与发声内容库（批次二十二 F260-F269）。静态内容只增不改顺序。
 */
public final class CoupleListenBank {

    private CoupleListenBank() {
    }

    /** F267 语气翻译条（自报语气 -> 给对方看的字幕）。 */
    public static final Map<String, String> TONE_LABEL = Map.of(
            "TIRED", "电量红了", "BUSY", "脚不沾地", "SAD", "心里在下雨", "OKAY", "其实没事");

    public static final Map<String, String> TONE_LINE = Map.of(
            "TIRED", "TA 今天回复短不是因为你，是电量见底了。给点安静，晚点会回血。",
            "BUSY", "TA 今天忙到飞起，已读不回≠不想理你，日程表背锅。",
            "SAD", "TA 今天情绪在下小雨，短句不是冷，是没力气解释。抱抱优先于讲理。",
            "OKAY", "TA 今天状态正常，短句只是省电模式，不用担心。");

    /** F268 休战到点卡文案。 */
    public static String truceEndCard(long seed) {
        return TRUCE_CARDS.get(Math.floorMod(seed, TRUCE_CARDS.size()));
    }

    private static final List<String> TRUCE_CARDS = List.of(
            "休战时间到。刚才那句最伤人的话，先让它过期，再决定要不要续费。",
            "30 分钟到了。气还没消可以续一面旗，但记得：家是讲爱的地方，不是讲赢的地方。",
            "停战钟响。谁先开口不算输，算长大。");

    /** F269 今日称呼池（日抛爱称）。 */
    public static String nameOfDay(long seed) {
        return NAMES.get(Math.floorMod(seed, NAMES.size()));
    }

    private static final List<String> NAMES = List.of(
            "饲养员", "置顶联系人", "人间闹钟", "专属树洞", "拥抱供应商", "零食合伙人",
            "最佳室友", "深夜食堂店长", "顺毛大师", "家庭弟位之首", "心尖尖同志", "今日限定宝贝",
            "吵架冠军(退让亚军)", "充电宝本宝", "我方的旗", "人间小满");

    /** F266 连续 21 天纪念卡。 */
    public static final String THREE_LINE_21 = "三行打卡连续 21 天啦！习惯说「被记住」，你们已经互相记住了 21 个今天 🎖️";

    /** F260 时段收尾互评语义。 */
    public static String rateLine(int score) {
        if (score >= 4) {
            return "被听见的感觉很好，下次还约";
        }
        if (score == 3) {
            return "听了一半，剩下一半下次补";
        }
        return "这次没聊透，别灰心，再练";
    }

    /** F265 放行送达话术。 */
    public static String holdDeliverLine() {
        return "有句「早想说」到期放行了，TA 憋了 7 天的话，先听完再回 🕊️";
    }
}
