package com.smart.chat.couple;

import java.util.List;

/** 生活经营系静态内容：家务积分可兑换的小奖励（F186，无表）。 */
public final class CoupleManageBank {

    private CoupleManageBank() {
    }

    /** 奖励：用赚来的家务积分兑换的浪漫小票。 */
    public record Reward(String code, String name, String emoji, int points) {
    }

    public static final List<Reward> REWARDS = List.of(
            new Reward("MOVIE", "电影之夜选片权", "🎬", 20),
            new Reward("DESERT", "免洗碗金牌一张", "🍰", 15),
            new Reward("BREAKFAST", "周末爱心早餐", "🍳", 25),
            new Reward("GAME", "游戏时间不限时", "🎮", 30),
            new Reward("HUG20", "二十分钟抱抱", "🤗", 10),
            new Reward("WISH", "任意愿望卡", "🌟", 50)
    );

    /** 按 code 查奖励，不存在返回 null。 */
    public static Reward rewardOf(String code) {
        return REWARDS.stream().filter(r -> r.code().equalsIgnoreCase(code == null ? "" : code.trim())).findFirst().orElse(null);
    }
}
