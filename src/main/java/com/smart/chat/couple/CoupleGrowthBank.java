package com.smart.chat.couple;
import java.util.List;
import java.util.Map;

/**
 * 共同养成内容库（系统裁剪后只剩「下次一定」的催办话术）。
 * 全部为内置静态内容，只增不改既有条目顺序。
 */
public final class CoupleGrowthBank {

    private CoupleGrowthBank() {
    }

    /** 「下次一定」催办文案。 */
    public static String nudgeLine(String content) {
        return "⏰ 你随口说过的「下次一定」被 TA 翻牌啦：「" + content + "」——今天兑现，明天下架";
    }
}