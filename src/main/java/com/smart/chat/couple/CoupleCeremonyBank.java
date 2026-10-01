package com.smart.chat.couple;

import java.util.List;

/** 小日子·仪式感系静态文案：今日宜/忌俏皮话、过法补催、保险柜 payout 券面、加冕开场（F231/F233/F234/F238，无表、只增不改）。 */
public final class CoupleCeremonyBank {

    private CoupleCeremonyBank() {
    }

    /** 老黄历「今日宜」俏皮话。 */
    public static final List<String> YI = List.of(
            "宜把小事过成节日，哪怕只是一起吃顿好的。",
            "宜提前庆祝，反正日子是好日子。",
            "宜牵手散步，宜当面夸人。",
            "宜给 TA 递一张愿望券，宜说话算数。",
            "宜早睡，宜想 TA，宜把灯一起熄掉。",
            "宜翻旧照片傻笑，宜聊「我们的小日子」。",
            "宜认认真真过一次节，仪式感也是爱意。",
            "宜交保险柜保费——夸 TA 一句，不亏。"
    );

    /** 老黄历「今日忌」俏皮话。 */
    public static final List<String> JI = List.of(
            "忌把「下次再约」说第三遍。",
            "忌庆祝时刷手机超过三分钟。",
            "忌忘记今天是谁的小日子。",
            "忌夸人吝啬，保费拖欠是要「退保」的。",
            "忌把纪念日过成走流程。",
            "忌吵架过夜，宜先递一句软话。",
            "忌许愿不兑现，愿望券也会伤心的。",
            "忌一个人庆祝，宜叫上 TA。"
    );

    /** 隔日未齐的补催话术（%s 为小日子名称）。 */
    public static final List<String> NUDGE = List.of(
            "昨天的「%s」还有过法没打勾呢——去年的今天你们可都完成了。",
            "「%s」的庆祝清单还差几笔勾，补上它，仪式感不欠账。",
            "Reminder：「%s」没过完～挑个晚上把它补齐，去年今日不该留尾巴。"
    );

    /** 保险柜满 N 个月的 payout 券面。 */
    public static String payoutTitle(int months) {
        return "保险柜 payout：" + months + " 个月保费交齐，凭此券许一个愿望（对方必须兑现）";
    }

    /** 年度加冕开场白。 */
    public static final List<String> CROWN_OPEN = List.of(
            "👑 加冕时刻：今年被你们认真过下来的小日子，前三名是——",
            "👑 年度热闹榜：把日子过成节的人，值得被加冕。",
            "👑 盘点今年最热闹的三个人造节日，掌声交给你们自己。"
    );

    /** 今日宜（按空间+日期稳定取一条）。 */
    public static String yi(String spaceId, String day) {
        return "今日宜：" + YI.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|ce-yi|" + day), YI.size()));
    }

    /** 今日忌（按空间+日期稳定取一条）。 */
    public static String ji(String spaceId, String day) {
        return "今日忌：" + JI.get(Math.floorMod(CoupleRitualBank.stableHash(spaceId + "|ce-ji|" + day), JI.size()));
    }

    /** 补催一句（按空间+小日子名稳定取一条）。 */
    public static String nudge(String spaceId, String foundedName) {
        String tpl = NUDGE.get(Math.floorMod(
                CoupleRitualBank.stableHash(spaceId + "|ce-nudge|" + foundedName), NUDGE.size()));
        return String.format(tpl, foundedName);
    }

    /** 加冕开场白（按空间+年份稳定取一条）。 */
    public static String crownOpen(String spaceId, int year) {
        return CROWN_OPEN.get(Math.floorMod(
                CoupleRitualBank.stableHash(spaceId + "|ce-crown|" + year), CROWN_OPEN.size()));
    }
}
