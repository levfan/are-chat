package com.smart.chat.couple;

import java.util.List;

/**
 * 周年抽奖箱内容库（系统裁剪后传世系统唯一保留项）。静态内容只增不改顺序。
 * 口径：奖位是「不用花钱但会笑」的小事，抽出来当天就能兑现。
 */
public final class CoupleLegacyBank {

    private CoupleLegacyBank() {
    }

    /** 奖池（迷你愿望位）——本年一条积分都没攒过时才回落到这里。 */
    public static final List<String> PRIZES = List.of(
            "一次「你说什么都对」的两小时", "一顿你选的餐厅，另一人不许挑",
            "一部对方想看很久但一直没看的片", "一次无理由的外卖代点",
            "一个周末早上不用定闹钟的特权", "一次免做家务金牌",
            "一封手写信，内容随你", "一次地点盲选：他定你不问");

    /** 周年抽奖提醒话术。 */
    public static String drawRemindLine() {
        return "周年到了：抽奖箱已开封，两人各抽一次，奖券不许退。";
    }
}
