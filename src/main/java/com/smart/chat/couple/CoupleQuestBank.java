package com.smart.chat.couple;

import java.util.List;

/**
 * 人生关卡内容库（批次三十三 F370-F379）。静态内容只增不改顺序。
 * 口径：「你的人生大事，我不缺席」做成系统，而不是靠记性——
 * 关卡预告、出关战报、加班预报、生病陪护、静音舱、搬家互助、低谷通行证、小胜利账本、关卡成就墙、下次关卡预约。
 * 文案按 seed（stableHash）稳定取值，同一关同一天翻出来是同一句话，无副作用。
 */
public final class CoupleQuestBank {

    private CoupleQuestBank() {
    }

    // ========== 字典 ==========

    /** F370 关卡类型中文名（未知类型回「其它」）。 */
    public static String kindLabel(String kind) {
        return switch (kind == null ? "" : kind) {
            case CoupleQuestBattle.KIND_INTERVIEW -> "面试";
            case CoupleQuestBattle.KIND_REPORT -> "汇报";
            case CoupleQuestBattle.KIND_DEFEND -> "答辩";
            case CoupleQuestBattle.KIND_TALK -> "谈判";
            case CoupleQuestBattle.KIND_CHECKUP -> "体检";
            default -> "其它";
        };
    }

    /** F371 战果中文名。 */
    public static String resultLabel(String result) {
        return switch (result == null ? "" : result) {
            case CoupleQuestReport.RESULT_WIN -> "漂亮通关";
            case CoupleQuestReport.RESULT_LOSE -> "没扛住";
            case CoupleQuestReport.RESULT_SURVIVE -> "活着回来了";
            default -> "打完了";
        };
    }

    /** F371 按战果定章名：WIN 庆功章 / SURVIVE 幸亏章 / LOSE 抱抱章。 */
    public static String sealLabel(String result) {
        return switch (result == null ? "" : result) {
            case CoupleQuestReport.RESULT_WIN -> "🏆 庆功章";
            case CoupleQuestReport.RESULT_SURVIVE -> "🍀 幸亏章";
            case CoupleQuestReport.RESULT_LOSE -> "🫂 抱抱章";
            default -> "🎖️ 到场章";
        };
    }

    // ========== F370/F371 关卡与战报 ==========

    /** F370 预告发出（推对方：我有一关要打）。 */
    public static String battlePrepLine(String kindLabel, String name, String day, String fear) {
        String tail = fear == null || fear.isBlank() ? "" : "，TA 说：「" + fear + "」";
        return "⚔️ " + day + " 有一场「" + kindLabel + "」——" + name + "。记一下，别到时候忘了问结果" + tail;
    }

    /** F371 战报发出（推对方：结果出来了）。 */
    public static String reportLine(String name, String resultLabel, String feeling) {
        String tail = feeling == null || feeling.isBlank() ? "" : "，一句感受：「" + feeling + "」";
        return "📣 「" + name + "」打完了：" + resultLabel + tail + "。去给 TA 盖个章吧。";
    }

    /** F371 盖章（推双方）。 */
    public static String sealLine(String name, String sealLabel) {
        return "🎖️ 「" + name + "」盖上 " + sealLabel + "——这一关不是一个人打的。";
    }

    // ========== F372 加班预报 ==========

    /** F372 预报发出（推对方：别等饭）。 */
    public static String overtimeLine(int untilHour, String note) {
        String tail = note == null || note.isBlank() ? "" : "，附一句：「" + note + "」";
        return "🌙 今晚要忙到 " + untilHour + " 点左右，别等饭" + tail;
    }

    /** F372 留灯卡（推加班的人）。 */
    public static String lampLine(String text) {
        return "💡 灯给你留着：「" + text + "」——回来再晚，屋里是亮的。";
    }

    // ========== F373 生病陪护 ==========

    /** F373 开单（推陪护的人：交给你了）。 */
    public static String nurseOpenLine(String symptom) {
        String tail = symptom == null || symptom.isBlank() ? "" : "：" + symptom;
        return "🤒 有人不舒服了" + tail + "。陪护单挂起来了，喝水吃药你代记。";
    }

    /** F373 代记打卡（推生病的人）。 */
    public static String careMarkLine(String kindLabel, int count) {
        return "💧 " + kindLabel + "已记上（这一单累计 " + count + " 次）。有人替你数着，病也好得快一点。";
    }

    /** F373 关单庆典（推双方）。 */
    public static String nurseCloseLine(long days, int waterCount, int medCount) {
        return "🎉 陪护单关了：陪了 " + days + " 天，记了 " + waterCount + " 次喝水、" + medCount
                + " 次吃药。病是 TA 生的，账是两个人一起记的。";
    }

    // ========== F374 静音舱 ==========

    /** F374 入舱（推对方：这段时间我只发加油卡）。 */
    public static String podInLine(String untilDay) {
        return "🔇 TA 进静音舱了，到 " + untilDay + " 出舱。这期间别催消息，想 TA 就发一张加油卡。";
    }

    /** F374 加油卡（推舱里的人）。 */
    public static String podCheerLine(long seed) {
        return pick(CHEERS, seed);
    }

    /** F374 出舱（推双方）+ 提醒对方补一封长信。 */
    public static String podOutLine(String letterTo) {
        return "🔔 出舱了！考完了、忙完了。@" + letterTo + " 说好的一封长信别忘了写——TA 憋了一舱的话也想跟你说。";
    }

    /** F374 长信已补（推双方）。 */
    public static String podLetterLine() {
        return "✉️ 长信已送达，静音舱的债清了。下次进舱，我继续在外面守着。";
    }

    // ========== F375 搬家互助 ==========

    /** F375 认领区块（推对方）。 */
    public static String moveClaimLine(String name, String owner) {
        return "📦 「" + name + "」这块 " + owner + " 认领了。剩下的格子谁来？";
    }

    /** F375 区块打包完（推双方）。 */
    public static String moveDoneLine(String name, int boxes) {
        return "✅ 「" + name + "」打包完了，" + boxes + " 箱。搬家这件事，我们又少怕了一点。";
    }

    /** F375 新家第一晚双打卡（推双方）。 */
    public static String moveNightLine(String day) {
        return "🏠 " + day + "，新家第一晚两个人都在。房子是空的，从这一晚开始是家的。";
    }

    // ========== F376 低谷通行证 ==========

    /** F376 宣布进低谷（推对方）。 */
    public static String valleyOpenLine(String untilDay, int span) {
        return "🌧️ TA 说最近状态不好，通行证挂到 " + untilDay + "（" + span + " 天）。不用讲道理，一天递一张「不说话也行」就好。";
    }

    /** F376 「不说话也行」卡（推低谷里的人）。 */
    public static String valleyCareLine(long seed) {
        return pick(CARE_CARDS, seed);
    }

    /** F376 宣布回升（推双方）。 */
    public static String valleyUpLine(int careCount, long spanDays) {
        return "🌤️ 通行证到期前 TA 自己说了「我缓过来了」。这 " + spanDays + " 天你递了 " + careCount
                + " 张卡，一句道理都没讲，但都在。";
    }

    // ========== F377 小胜利账本 ==========

    /** F377 记账（推对方：TA 做成了一件事）。 */
    public static String winLine(String content) {
        return "🏅 今天做成了一件小事：「" + content + "」。周日记得给 TA 颁小赢奖。";
    }

    /** F377 互颁小赢奖（推双方）。 */
    public static String awardLine(String content, String from) {
        return "🏆 本周小赢奖颁给：「" + content + "」。@" + from + " 这件事我看见了。";
    }

    // ========== F379 下次关卡预约 ==========

    /** F379 应援到场（推挂关卡的人）。 */
    public static String attendLine(String title, String day, String by) {
        return "🙋 " + day + " 的「" + title + "」——" + by + " 说：这一天我会到场。";
    }

    // ========== F378 关卡成就墙 ==========

    /** F378 年度称号（按打过的关卡数定档，越打越不是一个人）。 */
    public static String wallTitle(int battles, int attends) {
        if (battles >= 24 && attends >= 12) {
            return "人生关卡双人通关组";
        }
        if (battles >= 12) {
            return "并肩作战搭档";
        }
        if (battles >= 6) {
            return "随队后勤官";
        }
        if (battles >= 2) {
            return "举牌助威的人";
        }
        return "刚上场的新兵";
    }

    /** F378 成就墙收尾话术。 */
    public static String wallTail(long seed) {
        return pick(WALL_TAILS, seed);
    }

    /** F378 成就墙整段 summary。 */
    public static String wallSummary(String year, int battles, int reports, int winRate, int nurseDays,
                                     int pods, int valleyDays, int awards, int attends, long seed) {
        return "🧗 " + year + " 年人生关卡墙：上了 " + battles + " 场 Boss 战，交了 " + reports + " 份战报（通关率 "
                + winRate + "%），陪护 " + nurseDays + " 天，静音舱 " + pods + " 次，低谷通行证 " + valleyDays
                + " 天，互颁小赢奖 " + awards + " 次，到场应援 " + attends + " 回——称号「" + wallTitle(battles, attends)
                + "」。" + wallTail(seed);
    }

    // ========== 静态语料 ==========

    /** F374 加油卡话术池（8 条）。 */
    private static final List<String> CHEERS = List.of(
            "💪 加油卡：你在里面拼命，我在外面撑场，赢了我们一起吃好的。",
            "🍀 加油卡：会的全对，蒙的全中。不会的题它也不认识你，别慌。",
            "☕ 加油卡：别忘了喝水。考试是长跑，膀胱和胃都要管。",
            "🐢 加油卡：慢一点没关系，你已经在往前走了。",
            "🔔 加油卡：今天也辛苦了。出舱那天我准备了话，很长，你慢慢听。",
            "🌟 加油卡：你复习的样子我已经觉得很了不起了。",
            "🧊 加油卡：紧张就深呼吸三口，第一口归我，后面两口归你。",
            "🏠 加油卡：忙完回家，灯和饭我都盯着。"
    );

    /** F376 「不说话也行」卡话术池（8 条）。 */
    private static final List<String> CARE_CARDS = List.of(
            "🌧️ 不说话也行卡：今天不用回消息，我知道你在，就够了。",
            "🛋️ 不说话也行卡：我就坐在旁边，你想聊随时开口，不想聊我也不会走。",
            "🍲 不说话也行卡：饭给你点了，不用谢我，也不用说话。",
            "🌙 不说话也行卡：今晚早点睡，天大的事明天再塌一次。",
            "🧻 不说话也行卡：哭也行的。哭完记得喝口水。",
            "🚪 不说话也行卡：门我留着，你要是想出来，一抬头就看见我。",
            "🐤 不说话也行卡：不用解释为什么不开心。不解释也是一种被允许。",
            "⏳ 不说话也行卡：通行证还在有效期内，你不用急着好起来。"
    );

    /** F378 成就墙收尾句池（4 条）。 */
    private static final List<String> WALL_TAILS = List.of(
            "以后的大日子，还是这句：我在。",
            "关关卡卡都有人旁听，人生就没那么难打。",
            "这些数字不漂亮，但一个都不是 TA 一个人扛的。",
            "你上场，我举牌——这队伍配置挺合理的。"
    );

    private static String pick(List<String> pool, long seed) {
        return pool.get((int) Math.floorMod(seed, pool.size()));
    }
}
