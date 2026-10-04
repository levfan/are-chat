package com.smart.chat.couple.domain.streak;

/**
 * 连续互动打卡的解锁档位：七档各管一件「只有你们俩看得到」的东西。
 * <p>
 * 档位是产品事实，不是可调参数：天数与对外 key 一旦上线就冻结——前端按 key 挂视觉，
 * 改了 key 等于把已解锁用户的外观没收。{@link #days()} 只增不减地按历史最长连续判定。
 */
public enum StreakTier {

    /** 3 天：聊天气泡换成情侣款（旁人看不到） */
    BUBBLE(3, "bubble", "双人专属气泡", "🫧", "你们的消息气泡换成情侣款，只有你们俩看得到"),
    /** 7 天：空间背景解锁一张会长的动态背景 */
    BACKGROUND(7, "background", "空间背景", "🌱", "小空间有了动态背景，那棵你们一起养的植物发芽了"),
    /** 14 天：对方给你的爱称带光效 */
    NICKNAME_GLOW(14, "nickname-glow", "昵称特效", "✨", "TA 给你起的爱称开始发光，打开对话框就能看到"),
    /** 21 天：头像旁出现联动小挂件 */
    PENDANT(21, "pendant", "双人挂件", "🧸", "你们头像边上挂上了联动小挂件"),
    /** 30 天：恋爱等级称号显示到空间顶部 */
    TITLE(30, "title", "恋爱等级称号", "🏷️", "恋爱等级称号挂上空间顶部，旁边的人都能看到"),
    /** 50 天：一组只有你们能用的专属贴纸 */
    CUSTOM_EMOJI(50, "custom-emoji", "专属贴纸包", "🎨", "解锁一组只有你们俩能用的专属贴纸"),
    /** 100 天：隐藏页签，100 天回顾时间轴 + 一句话总结 */
    EASTER_EGG(100, "easter-egg", "隐藏彩蛋页", "🥚", "多了一个谁都不给看的隐藏页，里面是你们的一百天");

    private final int days;
    private final String key;
    private final String label;
    private final String icon;
    private final String detail;

    StreakTier(int days, String key, String label, String icon, String detail) {
        this.days = days;
        this.key = key;
        this.label = label;
        this.icon = icon;
        this.detail = detail;
    }

    public int days() {
        return days;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }

    public String detail() {
        return detail;
    }

    /** 对外 wire 值用的档位（按 key 查，找不到返回 null 由调用方判定）。 */
    public static StreakTier ofKey(String key) {
        for (StreakTier tier : values()) {
            if (tier.key.equals(key)) {
                return tier;
            }
        }
        return null;
    }
}
