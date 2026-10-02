package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * 人生关卡（批次三十三 F370-F379）：关卡预告、出关战报、加班预报、生病陪护单、考试周静音舱、
 * 搬家互助、低谷通行证、小胜利账本、关卡成就墙、下次关卡预约。
 * 情绪命题：「你的人生大事，我不缺席」要能落到系统里，而不是靠记性和嘴。
 * 口径：所有写接口原样返回整份 QuestVO 聚合（GET /board 的形状），前端整体替换；
 * 双人列（新家第一晚）沿用 userA/userB 口径；日期一律 yyyy-MM-dd，CSV 打卡位用 MMdd。
 */
@Service
public class CoupleQuestService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleQuestBattleMapper battleMapper;
    private final CoupleQuestReportMapper reportMapper;
    private final CoupleQuestOvertimeMapper overtimeMapper;
    private final CoupleQuestNurseMapper nurseMapper;
    private final CoupleQuestCareMarkMapper careMarkMapper;
    private final CoupleQuestPodMapper podMapper;
    private final CoupleQuestMoveMapper moveMapper;
    private final CoupleQuestMoveNightMapper moveNightMapper;
    private final CoupleQuestValleyMapper valleyMapper;
    private final CoupleQuestWinMapper winMapper;
    private final CoupleQuestUpcomingMapper upcomingMapper;
    private final ImPushService push;

    /** 列表钳制阈值（历史全量进 payload 会把总览越用越肥，沿用 v6 的口径）。 */
    private static final int LIST_BATTLE = 12;
    private static final int LIST_REPORT = 20;
    private static final int LIST_NURSE = 6;
    private static final int LIST_WIN = 21;
    private static final int LIST_UPCOMING = 20;
    private static final int LIST_POD = 8;

    private static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MMdd", Locale.ROOT);

    public CoupleQuestService(CoupleSpaceMapper spaceMapper, CoupleQuestBattleMapper battleMapper,
                              CoupleQuestReportMapper reportMapper, CoupleQuestOvertimeMapper overtimeMapper,
                              CoupleQuestNurseMapper nurseMapper, CoupleQuestCareMarkMapper careMarkMapper,
                              CoupleQuestPodMapper podMapper, CoupleQuestMoveMapper moveMapper,
                              CoupleQuestMoveNightMapper moveNightMapper, CoupleQuestValleyMapper valleyMapper,
                              CoupleQuestWinMapper winMapper, CoupleQuestUpcomingMapper upcomingMapper,
                              ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.battleMapper = battleMapper;
        this.reportMapper = reportMapper;
        this.overtimeMapper = overtimeMapper;
        this.nurseMapper = nurseMapper;
        this.careMarkMapper = careMarkMapper;
        this.podMapper = podMapper;
        this.moveMapper = moveMapper;
        this.moveNightMapper = moveNightMapper;
        this.valleyMapper = valleyMapper;
        this.winMapper = winMapper;
        this.upcomingMapper = upcomingMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F370 一关（在途或已打）。 */
    public record BattleVO(String id, String day, String kind, String kindLabel, String name, String fear,
                           boolean mine, boolean prep, int daysLeft, long created) {
    }

    /** F371 战报与盖章。 */
    public record ReportVO(String id, String battleId, String battleName, String result, String resultLabel,
                           String feeling, boolean mine, boolean sealed, String sealedBy, String sealLabel) {
    }

    /** F372 今晚的加班预报（含对方留的灯）。 */
    public record OvertimeVO(String id, int untilHour, String note, boolean mine, String lamp, String lampBy) {
    }

    /** F373 一次代记打卡。 */
    public record CareMarkVO(String day, String kind, String kindLabel, String byUser, boolean mine) {
    }

    /** F373 陪护单（含代记统计）。 */
    public record NurseVO(String id, String patientUser, String carerUser, boolean mineAsCarer, boolean open,
                          String openDay, String closeDay, String symptom, String message,
                          int waterCount, int medCount, long days, List<CareMarkVO> marks) {
    }

    /** F374 静音舱一行。 */
    public record PodVO(String id, boolean mine, String startDay, String untilDay, boolean in, int cheerCount,
                        boolean cheeredToday, boolean letterDone, long daysLeft) {
    }

    /** F375 搬家区块。 */
    public record MoveVO(String id, int slot, String name, String owner, boolean mine, boolean claimed,
                         boolean finished, int boxes) {
    }

    /** F375 新家第一晚。 */
    public record MoveNightVO(String id, String day, boolean mineTicked, boolean partnerTicked,
                              boolean bothTicked, String note) {
    }

    /** F376 低谷通行证。 */
    public record ValleyVO(String id, boolean mine, String openDay, String untilDay, boolean low, int careCount,
                           boolean caredToday, String reviveDay, int spanDays, long daysLeft) {
    }

    /** F377 小胜利。 */
    public record WinVO(String id, String day, boolean mine, String content, String awardDay, String awardedBy,
                        boolean awarded, boolean canAward) {
    }

    /** F379 下次关卡预约。 */
    public record UpcomingVO(String id, String day, String title, boolean mine, String attendBy, boolean attended,
                             int daysLeft) {
    }

    /** F378 关卡成就墙（年度聚合，数字全部来自真实表）。 */
    public record WallVO(int year, int battles, int reports, int winRate, int nurseDays, int pods, int valleyDays,
                         int awards, int attends, String title, String summary) {
    }

    /** 关卡总览：所有写接口都原样返回这份。 */
    public record QuestVO(String day, String weekStart, List<BattleVO> battles, List<ReportVO> reports,
                          OvertimeVO myOvertime, OvertimeVO partnerOvertime, boolean canLeaveLamp,
                          NurseVO myNurse, NurseVO partnerNurse, List<NurseVO> nurses,
                          PodVO myPod, PodVO partnerPod, List<MoveVO> moves, int moveBoxes,
                          MoveNightVO moveNight, ValleyVO myValley, ValleyVO partnerValley,
                          List<WinVO> wins, List<UpcomingVO> upcoming, WallVO wall) {
    }

    // ========== 读 ==========

    /** 关卡总览（GET /board）。 */
    public QuestVO board(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now());
    }

    /** F378 关卡成就墙（GET /wall?year=，year 缺省当年）。 */
    public WallVO wall(String me, String year) {
        CoupleSpace space = requireSpace(me);
        return wallOf(space, normalizeYear(year, LocalDate.now()));
    }

    // ========== F370 关卡预告 ==========

    /** F370 宣布一场 Boss 战（day 必须今天或以后，kind 白名单，name ≤30，fear ≤60，在途每人 ≤3）。 */
    public QuestVO addBattle(String me, String day, String kind, String name, String fear) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        LocalDate d = parseDay(day, "关卡日");
        if (d.isBefore(now)) {
            throw new BusinessException(400, "关卡日是过去的日子啦，要打就挑今天或以后 ⚔️");
        }
        String k = kind == null || kind.isBlank() ? CoupleQuestBattle.KIND_OTHER
                : kind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleQuestBattle.KINDS.contains(k)) {
            throw new BusinessException(400, "关卡类型只能是面试/汇报/答辩/谈判/体检/其它");
        }
        String n = trim(name, "关卡总得有个名字，比如「述职答辩」");
        if (n.length() > CoupleQuestBattle.NAME_MAX) {
            throw new BusinessException(400, "关卡名最多 " + CoupleQuestBattle.NAME_MAX + " 字");
        }
        String f = fear == null ? "" : fear.trim();
        if (f.length() > CoupleQuestBattle.FEAR_MAX) {
            throw new BusinessException(400, "怯场话最多 " + CoupleQuestBattle.FEAR_MAX + " 字");
        }
        String dText = d.toString();
        if (battleMapper.find(space.getId(), me, dText, n) != null) {
            throw new BusinessException(400, "这场已经挂过了，换个名字或换个日子 ⚔️");
        }
        if (battleMapper.countPrep(space.getId(), me) >= CoupleQuestBattle.IN_FLIGHT_MAX) {
            throw new BusinessException(400, "在途的关卡最多 " + CoupleQuestBattle.IN_FLIGHT_MAX + " 场，先打完再挂");
        }
        battleMapper.insert(CoupleQuestBattle.of(space.getId(), me, dText, k, n, f));
        push.pushCoupleEvent("quest-battle", me, space.partnerOf(me),
                CoupleQuestBank.battlePrepLine(CoupleQuestBank.kindLabel(k), n, dText, f));
        return build(space, me, now);
    }

    /** F370 取消自己挂的在途关卡（打完就不用再挂着了）。 */
    public QuestVO removeBattle(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestBattle battle = requireBattle(space, id);
        if (!battle.getFromUser().equals(me)) {
            throw new BusinessException(400, "这场是 TA 的关卡，只有 TA 自己能撤 ⚔️");
        }
        if (!battle.prep()) {
            throw new BusinessException(400, "这一关已经报过战报了，留着当记录吧");
        }
        battleMapper.deleteById(id);
        push.pushCoupleEvent("quest-battle-cancel", me, space.partnerOf(me),
                "🕊️ TA 撤掉了「" + nz(battle.getName()) + "」这一关——可能是改期了。");
        return build(space, me, now);
    }

    // ========== F371 出关战报 ==========

    /** F371 报战果（只有打这一关的人能报，一战一报；WIN/LOSE/SURVIVE；报完关卡转 DONE）。 */
    public QuestVO report(String me, String battleId, String result, String feeling) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestBattle battle = requireBattle(space, battleId);
        if (!battle.getFromUser().equals(me)) {
            throw new BusinessException(400, "这一关是 TA 打的，战报得 TA 来交 📣");
        }
        if (reportMapper.findByBattle(battleId) != null) {
            throw new BusinessException(400, "这一关已经交过战报了，一关一份");
        }
        String r = result == null ? "" : result.trim().toUpperCase(Locale.ROOT);
        if (!CoupleQuestReport.RESULTS.contains(r)) {
            throw new BusinessException(400, "战果只有三种：漂亮通关 / 没扛住 / 活着回来了");
        }
        String f = feeling == null ? "" : feeling.trim();
        if (f.length() > CoupleQuestReport.FEELING_MAX) {
            throw new BusinessException(400, "一句感受最多 " + CoupleQuestReport.FEELING_MAX + " 字");
        }
        reportMapper.insert(CoupleQuestReport.of(space.getId(), battleId, r, f));
        battle.done();
        battleMapper.updateById(battle);
        push.pushCoupleEvent("quest-report", me, space.partnerOf(me),
                CoupleQuestBank.reportLine(nz(battle.getName()), CoupleQuestBank.resultLabel(r), f));
        return build(space, me, now);
    }

    /** F371 对方按战果盖章（庆功/抱抱/幸亏；自己不能给自己盖，重复盖幂等返回）。 */
    public QuestVO seal(String me, String reportId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestReport row = requireReport(space, reportId);
        CoupleQuestBattle battle = battleMapper.selectById(row.getBattleId());
        String name = battle == null ? "这一关" : nz(battle.getName());
        if (row.sealed()) {
            return build(space, me, now);
        }
        if (me.equals(rowSealerOwner(row))) {
            throw new BusinessException(400, "战报是自己交的，章要 TA 来盖 🎖️");
        }
        row.seal(me);
        reportMapper.updateById(row);
        push.pushCoupleEventBoth("quest-seal", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.sealLine(name, CoupleQuestBank.sealLabel(nz(row.getResult()))));
        return build(space, me, now);
    }

    // ========== F372 加班预报 ==========

    /** F372 预报今晚忙到几点（每人每天一行可改写，13-23 钳制，note ≤40）。 */
    public QuestVO overtime(String me, Integer untilHour, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        int h = untilHour == null ? CoupleQuestOvertime.HOUR_DEFAULT
                : Math.max(CoupleQuestOvertime.HOUR_MIN, Math.min(CoupleQuestOvertime.HOUR_MAX, untilHour));
        String n = note == null ? "" : note.trim();
        if (n.length() > CoupleQuestOvertime.NOTE_MAX) {
            throw new BusinessException(400, "一句说明最多 " + CoupleQuestOvertime.NOTE_MAX + " 字");
        }
        CoupleQuestOvertime row = overtimeMapper.find(space.getId(), day, me);
        if (row == null) {
            row = CoupleQuestOvertime.of(space.getId(), day, me, h, n);
            overtimeMapper.insert(row);
        } else {
            row.setUntilHour(h);
            row.setNote(n);
            row.setUpdatedAt(System.currentTimeMillis());
            overtimeMapper.updateById(row);
        }
        push.pushCoupleEvent("quest-overtime", me, space.partnerOf(me),
                CoupleQuestBank.overtimeLine(h, n));
        return build(space, me, now);
    }

    /** F372 给对方留一张「到家灯给你留着」卡（只有对方能留，且 TA 今晚确实预报了加班）。 */
    public QuestVO leaveLamp(String me, String id, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestOvertime row = requireOvertime(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "灯是给加班的人留的，自己留不算 💡");
        }
        String t = trim(text, "灯下想留的那句话写一句");
        if (t.length() > CoupleQuestOvertime.LAMP_MAX) {
            throw new BusinessException(400, "灯卡最多 " + CoupleQuestOvertime.LAMP_MAX + " 字");
        }
        row.leaveLamp(me, t);
        overtimeMapper.updateById(row);
        push.pushCoupleEvent("quest-lamp", me, space.partnerOf(me), CoupleQuestBank.lampLine(t));
        return build(space, me, now);
    }

    // ========== F373 生病陪护单 ==========

    /** F373 为 TA 开陪护单（只有对方能开——生病的人自己顾不上记；在途每人 ≤1，症状 ≤60）。 */
    public QuestVO openNurse(String me, String symptom) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String s = symptom == null ? "" : symptom.trim();
        if (s.length() > CoupleQuestNurse.SYMPTOM_MAX) {
            throw new BusinessException(400, "症状一句话最多 " + CoupleQuestNurse.SYMPTOM_MAX + " 字");
        }
        String patient = space.partnerOf(me);
        if (nurseMapper.findOpen(space.getId(), patient) != null) {
            throw new BusinessException(400, "TA 的陪护单还在途，一张够了 🤒");
        }
        nurseMapper.insert(CoupleQuestNurse.of(space.getId(), patient, me, now.toString(), s));
        push.pushCoupleEvent("quest-nurse-open", me, patient, CoupleQuestBank.nurseOpenLine(s));
        return build(space, me, now);
    }

    /** F373 陪护人代记一次打卡（WATER/MED，一天每种只记一次）。 */
    public QuestVO careMark(String me, String nurseId, String kind) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestNurse nurse = requireNurse(space, nurseId);
        if (!nurse.open()) {
            throw new BusinessException(400, "这张单已经关了，不用继续记了");
        }
        if (!me.equals(nurse.getCarerUser())) {
            throw new BusinessException(400, "陪护单是 " + nz(nurse.getCarerUser()) + " 在陪，代记轮不到别人 💧");
        }
        String k = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleQuestCareMark.KINDS.contains(k)) {
            throw new BusinessException(400, "只能记喝水或吃药两种");
        }
        String day = now.toString();
        if (careMarkMapper.find(nurseId, day, k, me) != null) {
            throw new BusinessException(400, "今天的" + ("WATER".equals(k) ? "喝水" : "吃药") + "已经记过啦");
        }
        careMarkMapper.insert(CoupleQuestCareMark.of(space.getId(), nurseId, day, k, me));
        List<CoupleQuestCareMark> all = careMarkMapper.findByNurse(nurseId);
        push.pushCoupleEvent("quest-care-mark", me, nurse.getPatientUser(),
                CoupleQuestBank.careMarkLine("WATER".equals(k) ? "喝水" : "吃药", all.size()));
        return build(space, me, now);
    }

    /** F373 陪护人写/改病中留言（≤80 字，只有陪护人能写）。 */
    public QuestVO nurseMessage(String me, String nurseId, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestNurse nurse = requireNurse(space, nurseId);
        if (!nurse.open()) {
            throw new BusinessException(400, "单已经关了，留言就留在记录里吧");
        }
        if (!me.equals(nurse.getCarerUser())) {
            throw new BusinessException(400, "病中留言是陪护人写的 ✍️");
        }
        String t = trim(text, "留言写一句再存");
        if (t.length() > CoupleQuestNurse.MESSAGE_MAX) {
            throw new BusinessException(400, "留言最多 " + CoupleQuestNurse.MESSAGE_MAX + " 字");
        }
        nurse.setMessage(t);
        nurse.setUpdatedAt(System.currentTimeMillis());
        nurseMapper.updateById(nurse);
        push.pushCoupleEvent("quest-nurse-message", me, nurse.getPatientUser(), "💌 陪护留言：「" + t + "」");
        return build(space, me, now);
    }

    /** F373 病人自己宣布痊愈关单（陪护人不能替 TA 说好，只有双人才算关单庆典）。 */
    public QuestVO closeNurse(String me, String nurseId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestNurse nurse = requireNurse(space, nurseId);
        if (!nurse.open()) {
            return build(space, me, now);
        }
        if (!me.equals(nurse.getPatientUser())) {
            throw new BusinessException(400, "痊愈要病人自己说，陪护的人不能替 TA 宣布 🎉");
        }
        List<CoupleQuestCareMark> marks = careMarkMapper.findByNurse(nurseId);
        nurse.close(now.toString(), null);
        nurseMapper.updateById(nurse);
        long days = Math.max(1, daysBetween(nurse.getOpenDay(), now.toString()) + 1);
        int water = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_WATER.equals(m.getKind())).count();
        int med = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_MED.equals(m.getKind())).count();
        push.pushCoupleEventBoth("quest-nurse-close", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.nurseCloseLine(days, water, med));
        return build(space, me, now);
    }

    // ========== F374 考试周静音舱 ==========

    /** F374 宣布入舱（untilDay 必须晚于今天；一人同时只有一个舱在途）。 */
    public QuestVO enterPod(String me, String untilDay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        LocalDate until = parseDay(untilDay, "出舱日");
        if (!until.isAfter(now)) {
            throw new BusinessException(400, "出舱日要晚于今天，静音舱不能当天开了就关 🔇");
        }
        if (podMapper.findIn(space.getId(), me) != null) {
            throw new BusinessException(400, "你还在舱里，先出舱再进一次");
        }
        podMapper.insert(CoupleQuestPod.of(space.getId(), me, now.toString(), until.toString()));
        push.pushCoupleEvent("quest-pod-in", me, space.partnerOf(me), CoupleQuestBank.podInLine(until.toString()));
        return build(space, me, now);
    }

    /** F374 给对方发一张加油卡（只有舱外的人能发，每天一张，白名单通道不屏蔽私信）。 */
    public QuestVO cheerPod(String me, String podId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestPod pod = requirePod(space, podId);
        if (!pod.in()) {
            throw new BusinessException(400, "TA 已经出舱了，加油卡改成长信吧 🔔");
        }
        if (me.equals(pod.getFromUser())) {
            throw new BusinessException(400, "舱是 TA 进的，加油卡要从外面递进去 💪");
        }
        String mmdd = now.format(MMDD);
        if (pod.cheered(mmdd)) {
            throw new BusinessException(400, "今天这张加油卡已经递过了，一天一张");
        }
        pod.cheer(mmdd);
        pod.setUpdatedAt(System.currentTimeMillis());
        podMapper.updateById(pod);
        long seed = CoupleRitualBank.stableHash(space.getId() + "|pod-cheer|" + podId + "|" + mmdd);
        push.pushCoupleEvent("quest-pod-cheer", me, pod.getFromUser(), CoupleQuestBank.podCheerLine(seed));
        return build(space, me, now);
    }

    /** F374 本人出舱（到点或提前都行），出舱后提醒对方补一封长信。 */
    public QuestVO leavePod(String me, String podId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestPod pod = requirePod(space, podId);
        if (!pod.in()) {
            return build(space, me, now);
        }
        if (!me.equals(pod.getFromUser())) {
            throw new BusinessException(400, "舱里的人才能自己出舱 🔔");
        }
        pod.out();
        pod.setUpdatedAt(System.currentTimeMillis());
        podMapper.updateById(pod);
        push.pushCoupleEventBoth("quest-pod-out", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.podOutLine(space.partnerOf(me)));
        return build(space, me, now);
    }

    /** F374 对方标记「长信已补」（只有舱外的那个人能标记）。 */
    public QuestVO podLetterDone(String me, String podId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestPod pod = requirePod(space, podId);
        if (pod.in()) {
            throw new BusinessException(400, "TA 还在舱里，长信等出舱再写");
        }
        if (me.equals(pod.getFromUser())) {
            throw new BusinessException(400, "长信是对面那个人写的，自己不能替 TA 打勾 ✉️");
        }
        if (pod.getLetterDone() != null && pod.getLetterDone() == 1) {
            return build(space, me, now);
        }
        pod.setLetterDone(1);
        pod.setUpdatedAt(System.currentTimeMillis());
        podMapper.updateById(pod);
        push.pushCoupleEventBoth("quest-pod-letter", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.podLetterLine());
        return build(space, me, now);
    }

    // ========== F375 搬家互助 ==========

    /** F375 给某个区块起名（slot 1-8，name ≤20，谁都能补）。 */
    public QuestVO moveName(String me, Integer slot, String name) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int s = slot == null ? 0 : slot;
        if (s < CoupleQuestMove.SLOT_MIN || s > CoupleQuestMove.SLOT_MAX) {
            throw new BusinessException(400, "区块位只有 " + CoupleQuestMove.SLOT_MIN + "-" + CoupleQuestMove.SLOT_MAX + " 格");
        }
        String n = trim(name, "这块要装什么，写个名字");
        if (n.length() > CoupleQuestMove.NAME_MAX) {
            throw new BusinessException(400, "区块名最多 " + CoupleQuestMove.NAME_MAX + " 字");
        }
        CoupleQuestMove row = moveMapper.findBySlot(space.getId(), s);
        if (row == null) {
            row = CoupleQuestMove.of(space.getId(), s);
            row.setName(n);
            row.setOwner(me);
            moveMapper.insert(row);
        } else {
            row.setName(n);
            row.setUpdatedAt(System.currentTimeMillis());
            moveMapper.updateById(row);
        }
        push.pushCoupleEvent("quest-move-name", me, space.partnerOf(me),
                "📦 搬家区块第 " + s + " 格改成了「" + n + "」。");
        return build(space, me, now);
    }

    /** F375 认领区块（已被对方认领 400；自己认领过的再点一次是取消）。 */
    public QuestVO moveClaim(String me, Integer slot) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestMove row = requireMove(space, slot);
        String owner = row.getOwner();
        if (owner != null && !owner.isBlank() && !owner.equals(me)) {
            throw new BusinessException(400, "这一格 " + owner + " 已经认领了，换一格吧 📦");
        }
        if (owner != null && !owner.isBlank()) {
            row.setOwner(null);
            row.setDone(0);
        } else {
            row.setOwner(me);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        moveMapper.updateById(row);
        if (row.getOwner() != null) {
            push.pushCoupleEvent("quest-move-claim", me, space.partnerOf(me),
                    CoupleQuestBank.moveClaimLine(nz(row.getName()), me));
        } else {
            push.pushCoupleEvent("quest-move-unclaim", me, space.partnerOf(me),
                    "📦 「" + nz(row.getName()) + "」这一格 " + me + " 松手了，谁来接？");
        }
        return build(space, me, now);
    }

    /** F375 记这一格打包了几箱（只有认领人能填，0-99 钳制）。 */
    public QuestVO moveBoxes(String me, Integer slot, Integer boxes) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestMove row = requireMove(space, slot);
        if (row.getOwner() == null || !row.getOwner().equals(me)) {
            throw new BusinessException(400, "这一格还没归你认领，数不了箱 📦");
        }
        int b = boxes == null ? 0 : Math.max(0, Math.min(CoupleQuestMove.BOX_MAX, boxes));
        row.setBoxes(b);
        row.setUpdatedAt(System.currentTimeMillis());
        moveMapper.updateById(row);
        return build(space, me, now);
    }

    /** F375 这一格打包完成（只有认领人能勾，勾完推双方）。 */
    public QuestVO moveDone(String me, Integer slot) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestMove row = requireMove(space, slot);
        if (row.getOwner() == null || !row.getOwner().equals(me)) {
            throw new BusinessException(400, "谁认领的谁来勾完成 📦");
        }
        if (row.finished()) {
            return build(space, me, now);
        }
        row.setDone(1);
        row.setUpdatedAt(System.currentTimeMillis());
        moveMapper.updateById(row);
        push.pushCoupleEventBoth("quest-move-done", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.moveDoneLine(nz(row.getName()), row.getBoxes() == null ? 0 : row.getBoxes()));
        return build(space, me, now);
    }

    /** F375 新家第一晚打卡（双人才算庆祝；note ≤60 由第一个点的人写）。 */
    public QuestVO moveNight(String me, String day, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = parseDay(day, "第一晚是哪天").toString();
        String n = note == null ? "" : note.trim();
        if (n.length() > CoupleQuestMoveNight.NOTE_MAX) {
            throw new BusinessException(400, "那一晚的一句话最多 " + CoupleQuestMoveNight.NOTE_MAX + " 字");
        }
        boolean isA = space.getUserA().equals(me);
        CoupleQuestMoveNight row = moveNightMapper.findByDay(space.getId(), d);
        boolean changed;
        if (row == null) {
            row = CoupleQuestMoveNight.of(space.getId(), d);
            if (!n.isEmpty()) {
                row.setNote(n);
            }
            changed = row.tick(isA);
            moveNightMapper.insert(row);
        } else {
            // 「补话」要落库：原先只有 0→1 翻转才 update，于是点过之后再来写一句话会被静默丢弃
            // （界面给了成功提示，重进却什么都没有）。推送仍只在真翻转时发，不重复打扰。
            boolean noteChanged = !n.isEmpty() && !n.equals(row.getNote());
            if (noteChanged) {
                row.setNote(n);
            }
            changed = row.tick(isA);
            if (changed || noteChanged) {
                row.setUpdatedAt(System.currentTimeMillis());
                moveNightMapper.updateById(row);
            }
        }
        if (changed && row.bothTicked()) {
            push.pushCoupleEventBoth("quest-move-night", me, space.getUserA(), space.getUserB(),
                    CoupleQuestBank.moveNightLine(d));
        }
        return build(space, me, now);
    }

    // ========== F376 低谷通行证 ==========

    /** F376 本人宣布最近状态不好（7-30 天，在途每人 ≤1）。 */
    public QuestVO openValley(String me, String untilDay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        LocalDate until = parseDay(untilDay, "回升日");
        long span = until.toEpochDay() - now.toEpochDay();
        if (span < CoupleQuestValley.SPAN_MIN || span > CoupleQuestValley.SPAN_MAX) {
            throw new BusinessException(400, "通行证要挂 " + CoupleQuestValley.SPAN_MIN + "-"
                    + CoupleQuestValley.SPAN_MAX + " 天，太短像赌气，太长像放弃 🌧️");
        }
        if (valleyMapper.findLow(space.getId(), me) != null) {
            throw new BusinessException(400, "你的通行证还在有效期内，不用重复开");
        }
        valleyMapper.insert(CoupleQuestValley.of(space.getId(), me, now.toString(), until.toString()));
        push.pushCoupleEvent("quest-valley-open", me, space.partnerOf(me),
                CoupleQuestBank.valleyOpenLine(until.toString(), (int) span));
        return build(space, me, now);
    }

    /** F376 对方递一张「不说话也行」卡（一天一张，只有 TA 能递）。 */
    public QuestVO valleyCare(String me, String valleyId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestValley valley = requireValley(space, valleyId);
        if (!valley.low()) {
            throw new BusinessException(400, "TA 已经回升了，卡改天再递 🌤️");
        }
        if (me.equals(valley.getFromUser())) {
            throw new BusinessException(400, "通行证是 TA 开的，卡要从外面递进来 🧻");
        }
        String mmdd = now.format(MMDD);
        if (valley.cared(mmdd)) {
            throw new BusinessException(400, "今天的卡已经递过了，一天一张");
        }
        valley.care(mmdd);
        valley.setUpdatedAt(System.currentTimeMillis());
        valleyMapper.updateById(valley);
        long seed = CoupleRitualBank.stableHash(space.getId() + "|valley-care|" + valleyId + "|" + mmdd);
        push.pushCoupleEvent("quest-valley-care", me, valley.getFromUser(), CoupleQuestBank.valleyCareLine(seed));
        return build(space, me, now);
    }

    /** F376 本人宣布回升收尾（只有本人能定回升日，幂等）。 */
    public QuestVO valleyRise(String me, String valleyId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestValley valley = requireValley(space, valleyId);
        if (!valley.low()) {
            return build(space, me, now);
        }
        if (!me.equals(valley.getFromUser())) {
            throw new BusinessException(400, "缓没缓过来只有 TA 自己说了算，别人不能替 TA 宣布 🌤️");
        }
        valley.rise(now.toString());
        valley.setUpdatedAt(System.currentTimeMillis());
        valleyMapper.updateById(valley);
        long spanDays = Math.max(1, daysBetween(valley.getOpenDay(), now.toString()));
        push.pushCoupleEventBoth("quest-valley-up", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.valleyUpLine(valley.careCount(), spanDays));
        return build(space, me, now);
    }

    // ========== F377 小胜利账本 ==========

    /** F377 记今天做成的一件小事（每人每天一条，改写不重推，content ≤40）。 */
    public QuestVO addWin(String me, String content, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : parseDay(day, "日子").toString();
        if (d.compareTo(now.toString()) > 0) {
            throw new BusinessException(400, "小事要今天或以前做成了才算，先别预支 🏅");
        }
        String c = trim(content, "做成的一件小事写一句，比如「把简历改了」");
        if (c.length() > CoupleQuestWin.CONTENT_MAX) {
            throw new BusinessException(400, "一条小事最多 " + CoupleQuestWin.CONTENT_MAX + " 字");
        }
        boolean fresh = winMapper.find(space.getId(), d, me) == null;
        CoupleQuestWin row = winMapper.find(space.getId(), d, me);
        if (row == null) {
            row = CoupleQuestWin.of(space.getId(), d, me, c);
            winMapper.insert(row);
        } else {
            row.setContent(c);
            row.setUpdatedAt(System.currentTimeMillis());
            winMapper.updateById(row);
        }
        if (fresh) {
            push.pushCoupleEvent("quest-win", me, space.partnerOf(me), CoupleQuestBank.winLine(c));
        }
        return build(space, me, now);
    }

    /** F377 互颁小赢奖：只能颁对方的记录，一人一周一颁。 */
    public QuestVO awardWin(String me, String winId) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestWin row = requireWin(space, winId);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "小赢奖是颁给 TA 的，不能自颁 🏆");
        }
        LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String fromDay = weekStart.toString();
        String toDay = weekStart.plusDays(6).toString();
        boolean alreadyAwarded = winMapper.findByDayRange(space.getId(), fromDay, toDay).stream()
                .anyMatch(w -> me.equals(w.getAwardedBy()));
        if (alreadyAwarded) {
            throw new BusinessException(400, "这周你已经颁过一次小赢奖了，下周再来");
        }
        row.award(me, now.toString());
        row.setUpdatedAt(System.currentTimeMillis());
        winMapper.updateById(row);
        push.pushCoupleEventBoth("quest-win-award", me, space.getUserA(), space.getUserB(),
                CoupleQuestBank.awardLine(nz(row.getContent()), me));
        return build(space, me, now);
    }

    // ========== F379 下次关卡预约 ==========

    /** F379 挂一个未来 60 天内的关口（title ≤30，同人同日同名 400）。 */
    public QuestVO addUpcoming(String me, String day, String title) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        LocalDate d = parseDay(day, "关口日子");
        if (d.isBefore(now) || d.isAfter(now.plusDays(CoupleQuestUpcoming.WINDOW_DAYS))) {
            throw new BusinessException(400, "关口只挂今天起 " + CoupleQuestUpcoming.WINDOW_DAYS + " 天之内的 🙋");
        }
        String t = trim(title, "关口叫什么，写一句");
        if (t.length() > CoupleQuestUpcoming.TITLE_MAX) {
            throw new BusinessException(400, "关口名最多 " + CoupleQuestUpcoming.TITLE_MAX + " 字");
        }
        String dText = d.toString();
        if (upcomingMapper.find(space.getId(), dText, me, t) != null) {
            throw new BusinessException(400, "这个关口你已经挂过了");
        }
        upcomingMapper.insert(CoupleQuestUpcoming.of(space.getId(), dText, me, t));
        push.pushCoupleEvent("quest-upcoming", me, space.partnerOf(me),
                "🗓️ " + dText + " 有个「" + t + "」——TA 挂上来了，你可以点「我会到场」。");
        return build(space, me, now);
    }

    /** F379 对方点「我会到场」（只有非挂单人能点，重复点幂等）。 */
    public QuestVO attendUpcoming(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestUpcoming row = requireUpcoming(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "到场要对方来说，自己给自己应援不算 🙋");
        }
        if (row.attended()) {
            return build(space, me, now);
        }
        row.attend(me);
        row.setUpdatedAt(System.currentTimeMillis());
        upcomingMapper.updateById(row);
        push.pushCoupleEvent("quest-attend", me, row.getFromUser(),
                CoupleQuestBank.attendLine(nz(row.getTitle()), nz(row.getDay()), me));
        return build(space, me, now);
    }

    /** F379 撤掉自己挂的关口。 */
    public QuestVO removeUpcoming(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleQuestUpcoming row = requireUpcoming(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "这个关口是 TA 挂的，只有 TA 能撤");
        }
        upcomingMapper.deleteById(id);
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private QuestVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();

        List<BattleVO> battles = battleMapper.findBySpace(space.getId()).stream()
                .filter(CoupleQuestBattle::prep)
                .sorted((a, b) -> nz(a.getDay()).compareTo(nz(b.getDay())))
                .limit(LIST_BATTLE)
                .map(b -> new BattleVO(b.getId(), nz(b.getDay()), nz(b.getKind()),
                        CoupleQuestBank.kindLabel(b.getKind()), nz(b.getName()), nz(b.getFear()),
                        me.equals(b.getFromUser()), b.prep(), (int) daysBetween(day, nz(b.getDay())),
                        b.getCreated() == null ? 0 : b.getCreated()))
                .toList();

        List<CoupleQuestBattle> allBattles = battleMapper.findBySpace(space.getId());
        java.util.Map<String, CoupleQuestBattle> battleById = new java.util.HashMap<>();
        for (CoupleQuestBattle b : allBattles) {
            battleById.put(b.getId(), b);
        }
        List<ReportVO> reports = reportMapper.findBySpace(space.getId()).stream()
                .limit(LIST_REPORT)
                .map(r -> {
                    CoupleQuestBattle b = battleById.get(r.getBattleId());
                    boolean mine = b != null && me.equals(b.getFromUser());
                    return new ReportVO(r.getId(), nz(r.getBattleId()), b == null ? "这一关" : nz(b.getName()),
                            nz(r.getResult()), CoupleQuestBank.resultLabel(r.getResult()), nz(r.getFeeling()),
                            mine, r.sealed(), nz(r.getSealedBy()), CoupleQuestBank.sealLabel(r.getResult()));
                })
                .toList();

        List<CoupleQuestOvertime> todays = overtimeMapper.findByDay(space.getId(), day);
        OvertimeVO myOvertime = null;
        OvertimeVO partnerOvertime = null;
        for (CoupleQuestOvertime o : todays) {
            OvertimeVO vo = new OvertimeVO(o.getId(), o.getUntilHour() == null ? CoupleQuestOvertime.HOUR_DEFAULT
                    : o.getUntilHour(), nz(o.getNote()), me.equals(o.getFromUser()), nz(o.getLamp()), nz(o.getLampBy()));
            if (me.equals(o.getFromUser())) {
                myOvertime = vo;
            } else {
                partnerOvertime = vo;
            }
        }

        NurseVO myNurse = toNurse(nurseMapper.findOpen(space.getId(), me), me);
        NurseVO partnerNurse = toNurse(nurseMapper.findOpen(space.getId(), space.partnerOf(me)), me);
        List<NurseVO> nurses = nurseMapper.findBySpace(space.getId()).stream()
                .limit(LIST_NURSE)
                .map(n -> toNurse(n, me))
                .toList();

        List<CoupleQuestPod> pods = podMapper.findBySpace(space.getId()).stream().limit(LIST_POD).toList();
        PodVO myPod = pods.stream().filter(p -> me.equals(p.getFromUser())).findFirst().map(p -> toPod(p, me, now))
                .orElse(null);
        PodVO partnerPod = pods.stream().filter(p -> !me.equals(p.getFromUser())).findFirst().map(p -> toPod(p, me, now))
                .orElse(null);

        List<MoveVO> moves = moveMapper.findBySpace(space.getId()).stream()
                .sorted((a, b) -> Integer.compare(a.getSlot() == null ? 0 : a.getSlot(),
                        b.getSlot() == null ? 0 : b.getSlot()))
                .limit(CoupleQuestMove.SLOT_MAX)
                .map(m -> new MoveVO(m.getId(), m.getSlot() == null ? 0 : m.getSlot(), nz(m.getName()),
                        nz(m.getOwner()), me.equals(m.getOwner()), m.claimed(), m.finished(),
                        m.getBoxes() == null ? 0 : m.getBoxes()))
                .toList();
        int moveBoxes = moves.stream().mapToInt(MoveVO::boxes).sum();

        // 新家第一晚：优先看今天或最近将来的那一晚，都没到点就回看最近一晚
        List<CoupleQuestMoveNight> nightRows = moveNightMapper.findBySpace(space.getId()).stream()
                .sorted(java.util.Comparator.comparing(n -> nz(n.getDay())))
                .toList();
        CoupleQuestMoveNight nightRow = nightRows.stream()
                .filter(n -> nz(n.getDay()).compareTo(day) >= 0)
                .findFirst()
                .orElse(nightRows.isEmpty() ? null : nightRows.get(nightRows.size() - 1));
        boolean isA = space.getUserA().equals(me);
        MoveNightVO moveNight = nightRow == null ? null : new MoveNightVO(nightRow.getId(), nz(nightRow.getDay()),
                nightRow.ticked(isA), nightRow.ticked(!isA), nightRow.bothTicked(), nz(nightRow.getNote()));

        ValleyVO myValley = toValley(valleyMapper.findLow(space.getId(), me), me, now);
        ValleyVO partnerValley = toValley(valleyMapper.findLow(space.getId(), space.partnerOf(me)), me, now);

        List<WinVO> wins = winMapper.findBySpace(space.getId()).stream()
                .limit(LIST_WIN)
                .map(w -> new WinVO(w.getId(), nz(w.getDay()), me.equals(w.getFromUser()), nz(w.getContent()),
                        nz(w.getAwardDay()), nz(w.getAwardedBy()), w.awarded(),
                        !me.equals(w.getFromUser()) && w.getFromUser().equals(space.partnerOf(me))))
                .toList();

        List<UpcomingVO> upcoming = upcomingMapper.findBySpace(space.getId()).stream()
                .filter(u -> u.getDay() != null && u.getDay().compareTo(day) >= 0)
                .limit(LIST_UPCOMING)
                .map(u -> new UpcomingVO(u.getId(), nz(u.getDay()), nz(u.getTitle()), me.equals(u.getFromUser()),
                        nz(u.getAttendBy()), u.attended(), (int) daysBetween(day, nz(u.getDay()))))
                .toList();

        return new QuestVO(day, weekStart, battles, reports, myOvertime, partnerOvertime,
                partnerOvertime != null && (partnerOvertime.lampBy == null || partnerOvertime.lampBy.isEmpty()),
                myNurse, partnerNurse, nurses, myPod, partnerPod, moves, moveBoxes, moveNight,
                myValley, partnerValley, wins, upcoming, wallOf(space, String.valueOf(now.getYear())));
    }

    private NurseVO toNurse(CoupleQuestNurse nurse, String me) {
        if (nurse == null) {
            return null;
        }
        List<CoupleQuestCareMark> marks = careMarkMapper.findByNurse(nurse.getId());
        int water = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_WATER.equals(m.getKind())).count();
        int med = (int) marks.stream().filter(m -> CoupleQuestCareMark.KIND_MED.equals(m.getKind())).count();
        String endDay = nurse.open() ? LocalDate.now().toString() : nz(nurse.getCloseDay());
        long days = Math.max(1, daysBetween(nurse.getOpenDay(), endDay) + 1);
        List<CareMarkVO> markVos = marks.stream()
                .map(m -> new CareMarkVO(nz(m.getDay()), nz(m.getKind()),
                        CoupleQuestCareMark.KIND_WATER.equals(m.getKind()) ? "喝水" : "吃药", nz(m.getByUser()),
                        me.equals(m.getByUser())))
                .toList();
        return new NurseVO(nurse.getId(), nz(nurse.getPatientUser()), nz(nurse.getCarerUser()),
                me.equals(nurse.getCarerUser()), nurse.open(), nz(nurse.getOpenDay()), nz(nurse.getCloseDay()),
                nz(nurse.getSymptom()), nz(nurse.getMessage()), water, med, days, markVos);
    }

    private PodVO toPod(CoupleQuestPod pod, String me, LocalDate now) {
        String mmdd = now.format(MMDD);
        return new PodVO(pod.getId(), me.equals(pod.getFromUser()), nz(pod.getStartDay()), nz(pod.getUntilDay()),
                pod.in(), pod.cheerCount(), pod.cheered(mmdd), pod.getLetterDone() != null && pod.getLetterDone() == 1,
                pod.in() ? Math.max(0, daysBetween(now.toString(), nz(pod.getUntilDay()))) : 0);
    }

    private ValleyVO toValley(CoupleQuestValley valley, String me, LocalDate now) {
        if (valley == null) {
            return null;
        }
        String mmdd = now.format(MMDD);
        return new ValleyVO(valley.getId(), me.equals(valley.getFromUser()), nz(valley.getOpenDay()),
                nz(valley.getUntilDay()), valley.low(), valley.careCount(), valley.cared(mmdd),
                nz(valley.getReviveDay()), (int) daysBetween(nz(valley.getOpenDay()), nz(valley.getUntilDay())),
                valley.low() ? Math.max(0, daysBetween(now.toString(), nz(valley.getUntilDay()))) : 0);
    }

    /** F378 成就墙：数字全部直接查原始表（不能用已钳制的列表计数，否则年报会被列表截小）。 */
    private WallVO wallOf(CoupleSpace space, String y) {
        List<CoupleQuestBattle> battles = battleMapper.findBySpace(space.getId()).stream()
                .filter(b -> yearOf(b.getDay()) == Integer.parseInt(y))
                .toList();
        List<CoupleQuestReport> reports = reportMapper.findBySpace(space.getId()).stream()
                .filter(r -> {
                    CoupleQuestBattle b = battleMapper.selectById(r.getBattleId());
                    return b != null && yearOf(b.getDay()) == Integer.parseInt(y);
                })
                .toList();
        int winCount = (int) reports.stream().filter(r -> CoupleQuestReport.RESULT_WIN.equals(r.getResult())).count();
        int rate = reports.isEmpty() ? 0 : (int) Math.round(winCount * 100.0 / reports.size());
        int nurseDays = 0;
        for (CoupleQuestNurse n : nurseMapper.findBySpace(space.getId())) {
            if (yearOf(n.getOpenDay()) != Integer.parseInt(y)) {
                continue;
            }
            String end = n.open() ? LocalDate.now().toString() : nz(n.getCloseDay());
            nurseDays += (int) Math.max(1, daysBetween(n.getOpenDay(), end));
        }
        int pods = (int) podMapper.findBySpace(space.getId()).stream()
                .filter(p -> yearOf(p.getStartDay()) == Integer.parseInt(y)).count();
        int valleyDays = 0;
        int cares = 0;
        for (CoupleQuestValley v : valleyMapper.findBySpace(space.getId())) {
            if (yearOf(v.getOpenDay()) != Integer.parseInt(y)) {
                continue;
            }
            String end = v.low() ? LocalDate.now().toString() : nz(v.getReviveDay());
            valleyDays += (int) Math.max(1, daysBetween(v.getOpenDay(), end));
            cares += v.careCount();
        }
        int awards = (int) winMapper.findBySpace(space.getId()).stream()
                .filter(w -> w.awarded() && yearOf(w.getAwardDay()) == Integer.parseInt(y)).count();
        int attends = (int) upcomingMapper.findBySpace(space.getId()).stream()
                .filter(u -> u.attended() && yearOf(u.getDay()) == Integer.parseInt(y)).count();
        int movesDone = (int) moveMapper.findBySpace(space.getId()).stream()
                .filter(CoupleQuestMove::finished).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|quest-wall|" + y);
        String summary = CoupleQuestBank.wallSummary(y, battles.size(), reports.size(), rate, nurseDays,
                pods + movesDone, valleyDays, awards + cares, attends, seed);
        return new WallVO(Integer.parseInt(y), battles.size(), reports.size(), rate, nurseDays, pods + movesDone,
                valleyDays, awards + cares, attends, CoupleQuestBank.wallTitle(battles.size(), attends), summary);
    }

    // ========== 小件 ==========

    private CoupleQuestBattle requireBattle(CoupleSpace space, String id) {
        CoupleQuestBattle row = id == null || id.isBlank() ? null : battleMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这一场关卡，可能已经撤掉了 ⚔️");
        }
        return row;
    }

    private CoupleQuestReport requireReport(CoupleSpace space, String id) {
        CoupleQuestReport row = id == null || id.isBlank() ? null : reportMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这份战报");
        }
        return row;
    }

    private CoupleQuestOvertime requireOvertime(CoupleSpace space, String id) {
        CoupleQuestOvertime row = id == null || id.isBlank() ? null : overtimeMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到今晚的加班预报，先让 TA 报一下 🌙");
        }
        return row;
    }

    private CoupleQuestNurse requireNurse(CoupleSpace space, String id) {
        CoupleQuestNurse row = id == null || id.isBlank() ? null : nurseMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这张陪护单 🤒");
        }
        return row;
    }

    private CoupleQuestPod requirePod(CoupleSpace space, String id) {
        CoupleQuestPod row = id == null || id.isBlank() ? null : podMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这个静音舱 🔇");
        }
        return row;
    }

    private CoupleQuestValley requireValley(CoupleSpace space, String id) {
        CoupleQuestValley row = id == null || id.isBlank() ? null : valleyMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这张通行证 🌧️");
        }
        return row;
    }

    private CoupleQuestWin requireWin(CoupleSpace space, String id) {
        CoupleQuestWin row = id == null || id.isBlank() ? null : winMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条小胜利 🏅");
        }
        return row;
    }

    private CoupleQuestUpcoming requireUpcoming(CoupleSpace space, String id) {
        CoupleQuestUpcoming row = id == null || id.isBlank() ? null : upcomingMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这个关口预约 🙋");
        }
        return row;
    }

    private CoupleQuestMove requireMove(CoupleSpace space, Integer slot) {
        int s = slot == null ? 0 : slot;
        if (s < CoupleQuestMove.SLOT_MIN || s > CoupleQuestMove.SLOT_MAX) {
            throw new BusinessException(400, "区块位只有 " + CoupleQuestMove.SLOT_MIN + "-"
                    + CoupleQuestMove.SLOT_MAX + " 格");
        }
        CoupleQuestMove row = moveMapper.findBySlot(space.getId(), s);
        if (row == null) {
            row = CoupleQuestMove.of(space.getId(), s);
            moveMapper.insert(row);
        }
        return row;
    }

    /** 战报是谁交的（盖章归属判断用：交给战报对应的关卡记录人）。 */
    private String rowSealerOwner(CoupleQuestReport row) {
        CoupleQuestBattle b = battleMapper.selectById(row.getBattleId());
        return b == null ? "" : nz(b.getFromUser());
    }

    private LocalDate parseDay(String day, String what) {
        String d = day == null ? "" : day.trim();
        if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(400, what + "写成 yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(d);
        } catch (RuntimeException ex) {
            throw new BusinessException(400, what + "写成 yyyy-MM-dd");
        }
    }

    private String normalizeYear(String year, LocalDate now) {
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        return y;
    }

    /** 两个日期串相差几天（解析不了给 0，正负都按原样给，调用处自己取绝对值）。 */
    private long daysBetween(String fromDay, String toDay) {
        try {
            return LocalDate.parse(toDay).toEpochDay() - LocalDate.parse(fromDay).toEpochDay();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private int yearOf(String day) {
        if (day == null || day.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(day.substring(0, 4));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
