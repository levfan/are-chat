package com.smart.chat.couple;

import java.util.List;

/** 体温同步系静态文案：喝水轻提醒、熬夜陪伴卡、叮嘱添衣话术（F223/F225/F224，无表、只增不改）。 */
public final class CoupleCozyBank {

    private CoupleCozyBank() {
    }

    /** 对方 3h 没回你一杯水时的一句轻提醒。 */
    public static final List<String> WATER_NUDGE = List.of(
            "你的水杯空了一格啦，要不要也喝一口？💧",
            "TA 已经喝了好几杯，你的一杯在哪呢？",
            "喝水接力棒回到你手上咯～",
            "别忘了润润嗓子，我在这头陪着你。",
            "接一杯温水吧，身体会谢谢你的。",
            "你的杯子在等你，就像我在等你。"
    );

    /** 深夜递上的「早点睡」陪伴卡文案。 */
    public static final List<String> LATENIGHT_CARD = List.of(
            "🌙 已经很晚啦，我把灯替你关了，早点睡。",
            "🥱 明天的事交给明天的你，今晚先睡个好觉。",
            "🛌 盖好被子，手机交给我保管一夜。",
            "💤 熬夜伤的是自己，我会心疼，去睡吧。",
            "🌌 星星都替你值完班了，你也该休息啦。",
            "🫖 睡前的温水我记下了，现在闭眼。"
    );

    /** 天气冷暖时叮嘱添衣的话术。 */
    public static final List<String> ADVISE_LINES = List.of(
            "降温了，记得加件外套，别只要风度 🧥",
            "手冷的话，戴上手套，或者…揣我兜里 🧤",
            "多喝热水不是敷衍，是真的想你别难受 ☕",
            "风大，围巾我给你想好了 🧣",
            "冷就承认嘛，我又不会笑你，加衣服！"
    );

    private static String pick(List<String> pool, String key) {
        return pool.get(Math.floorMod(CoupleRitualBank.stableHash(key), pool.size()));
    }

    /** 今日喝水轻提醒（按空间+日稳定）。 */
    public static String waterNudge(String spaceId, String day) {
        return pick(WATER_NUDGE, spaceId + "|water|" + day);
    }

    /** 今日熬夜陪伴卡文案（按空间+日+人稳定）。 */
    public static String latenightCard(String spaceId, String day, String fromUser) {
        return pick(LATENIGHT_CARD, spaceId + "|night|" + day + "|" + fromUser);
    }

    /** 叮嘱添衣话术（按空间+日+人稳定）。 */
    public static String adviseLine(String spaceId, String day, String fromUser) {
        return pick(ADVISE_LINES, spaceId + "|advise|" + day + "|" + fromUser);
    }
}
