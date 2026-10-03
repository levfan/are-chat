package com.smart.chat.couple;

import java.util.List;

/**
 * 家务轮盘内容库（系统裁剪后二人制造厂唯一保留项）。静态内容只增不改顺序。
 * 口径：转盘的胜负感靠话术兜着，别让它读起来像摊派。
 */
public final class CoupleFactoryBank {

    private CoupleFactoryBank() {
    }

    /** 轮盘开场话术池。 */
    public static String spinOpenLine(long seed) {
        return SPIN_OPEN.get(Math.floorMod(seed, SPIN_OPEN.size()));
    }

    private static final List<String> SPIN_OPEN = List.of(
            "命运转盘开始转动，本周家务听天由命 🎡",
            "愿者上钩，不服下周再转。",
            "转盘无罪，干活有理。",
            "抽到就是天选打工人，恭喜上岗。");
}
