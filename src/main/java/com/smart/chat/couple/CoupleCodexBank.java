package com.smart.chat.couple;

import java.util.List;
import java.util.Map;

/**
 * 喜好 TOP10 互猜内容库（系统裁剪后我们百科唯一保留项）。静态内容只增不改顺序。
 * 口径：八个类目都是「一口就能答上、但要猜准 TA 现在的口味可不容易」的小问题。
 */
public final class CoupleCodexBank {

    private CoupleCodexBank() {
    }

    /** 类目常量（键→展示名，只增不改序）。 */
    public static final List<String> TOP_CATEGORIES =
            List.of("FOOD", "MOVIE", "SONG", "COLOR", "PLACE_EAT", "SHOW", "SEAT", "SNACK");
    public static final Map<String, String> TOP_LABELS = Map.of(
            "FOOD", "爱吃 Top10", "MOVIE", "爱看影片 Top10", "SONG", "循环歌单 Top10",
            "COLOR", "心动颜色 Top10", "PLACE_EAT", "想约的店 Top10", "SHOW", "爱看的剧 Top10",
            "SEAT", "家里最爱待的角落 Top10", "SNACK", "冰箱常客 Top10");

    /** 重新认识清单话术——猜漏的那几项。 */
    public static String rematchLine(String category, String item) {
        return TOP_LABELS.getOrDefault(category, category) + "：「" + item + "」——原来 TA 现在喜欢这个";
    }
}
