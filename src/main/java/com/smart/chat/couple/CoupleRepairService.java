package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 修复车间（F320-F329，批次二十八）：冷冻解冻规程、道歉质检、重来卡、信任重建 30 天、
 * 和好了倒计时、冲突年报、底线声明卡、我错了榜、修复礼盒、和平纪念碑。
 * 情绪价值设计：吵架不可怕，可怕的是没有修好的路子——这里给的是流程、台阶和留档，
 * 让「我们和好了」每次都留下可回看的证据。
 */
@Service
public class CoupleRepairService {

    static final int FREEZE_MIN_HOURS = 3;
    static final int FREEZE_MAX_HOURS = 24;
    static final int REASON_MAX = 140;
    static final int ANSWER_MAX = 140;
    static final int LETTER_MAX = 300;
    static final int VERDICT_MAX = 80;
    static final int POINTS_MIN = 3;
    static final int SCENE_MAX = 140;
    static final int REPLAY_MAX = 200;
    static final int PLAN_NAME_MAX = 40;
    static final int TASK_MAX = 60;
    static final int TASK_LIMIT = 10;
    static final int REVIEW_MAX = 200;
    static final int MINUTE_MIN = 10;
    static final int MINUTE_MAX = 60;
    static final int LINE_TEXT_MAX = 60;
    static final int BREACH_NOTE_MAX = 80;
    static final int DETAIL_MAX = 140;
    static final int BOX_TASK_MAX = 80;
    static final int PEACE_MAX = 140;
    static final int PEACE_NOTE_MAX = 80;
    /** 档位限 14/30：signed_days 存 MMdd:A/MMdd:B 记号，30 天双签正好落在列宽内。 */
    static final List<Integer> TARGET_DAYS = List.of(14, 30);
    static final int LIST_FREEZE_MAX = 30;
    static final int LIST_SORRY_MAX = 20;
    static final int LIST_ADMIT_MAX = 40;
    static final int LIST_PEACE_MAX = 30;
    static final int LIST_BOX_MAX = 20;
    static final int LIST_PLAN_MAX = 12;
    static final int THAW_EARN_POINTS = 8;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleRepairFreezeMapper freezeMapper;
    private final CoupleSorryReviewMapper sorryMapper;
    private final CoupleRepairRedoMapper redoMapper;
    private final CoupleRebuildPlanMapper rebuildMapper;
    private final CoupleRepairMakeupMapper makeupMapper;
    private final CoupleBottomLineMapper bottomMapper;
    private final CoupleAdmitLogMapper admitMapper;
    private final CoupleRepairBoxMapper boxMapper;
    private final CouplePeaceLineMapper peaceMapper;
    private final CoupleReconcileMapper reconcileMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleRepairService(CoupleSpaceMapper spaceMapper, CoupleRepairFreezeMapper freezeMapper,
                               CoupleSorryReviewMapper sorryMapper, CoupleRepairRedoMapper redoMapper,
                               CoupleRebuildPlanMapper rebuildMapper, CoupleRepairMakeupMapper makeupMapper,
                               CoupleBottomLineMapper bottomMapper, CoupleAdmitLogMapper admitMapper,
                               CoupleRepairBoxMapper boxMapper, CouplePeaceLineMapper peaceMapper,
                               CoupleReconcileMapper reconcileMapper, CouplePointLedgerMapper ledgerMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.freezeMapper = freezeMapper;
        this.sorryMapper = sorryMapper;
        this.redoMapper = redoMapper;
        this.rebuildMapper = rebuildMapper;
        this.makeupMapper = makeupMapper;
        this.bottomMapper = bottomMapper;
        this.admitMapper = admitMapper;
        this.boxMapper = boxMapper;
        this.peaceMapper = peaceMapper;
        this.reconcileMapper = reconcileMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record FreezeVO(String id, String day, boolean mine, int hours, String status, long minutesLeft,
                           String reason, String answer1, String answer2, String answer3,
                           boolean signedMe, boolean signedPartner, boolean bothSigned, boolean questionsDone,
                           boolean canSign) {
    }

    public record SorryVO(String id, boolean mine, String letter, List<String> points, String status,
                          String verdict, String verifiedBy, boolean canVerify) {
    }

    public record RedoVO(String quarter, boolean mine, String scene, boolean used, String replayNote,
                         Integer satisfaction, String ratedBy, boolean canPlay, boolean canRate) {
    }

    public record TaskVO(int seq, String text) {
    }

    public record RebuildVO(String id, String name, boolean mine, String cause, String startDay, int targetDays,
                            List<TaskVO> tasks, int signedCount, int dayNo, String review, String status) {
    }

    public record MakeupVO(String id, String day, boolean mine, int minutes, String status, long secondsLeft,
                           boolean paused, String pausedBy, String stepCard, boolean canToggle, boolean canEnd) {
    }

    public record BottomVO(String id, boolean mine, int slot, String text, String sinceDay,
                           int breachCount, String breachNote, boolean canBreach) {
    }

    public record AdmitVO(String id, String day, boolean mine, String aboutUser, String detail,
                          boolean touched, boolean canTouch) {
    }

    public record BoxVO(String id, String day, boolean mine, String task, String status, String doneLine) {
    }

    public record PeaceVO(String id, String day, boolean mine, String line, String note) {
    }

    public record ReportVO(String year, int freezes, int thawed, int sorryIn, int passed, int backed,
                           int admits, int touched, int redos, int boxesDone, int peaceLines,
                           int reconciles, int reconcileAccepted, int avgThawHours, String prize,
                           String summary) {
    }

    public record RepairVO(String day, String quarter, String year, List<FreezeVO> freezes, FreezeVO current,
                           List<SorryVO> sorries, RedoVO redo, List<RebuildVO> rebuilds, MakeupVO makeup,
                           List<MakeupVO> makeups, List<BottomVO> bottoms, List<AdmitVO> admits,
                           List<BoxVO> boxes, List<PeaceVO> peace, ReportVO report) {
    }

    // ========== 读：修复车间总览（含惰性结算：倒计时到点递台阶卡） ==========

    /** 修复车间总览。 */
    public RepairVO workshop(String me) {
        CoupleSpace space = requireSpace(me);
        settle(space, System.currentTimeMillis());
        return build(space, me, LocalDate.now());
    }

    // ========== F320 冷冻解冻规程 ==========

    /** 挂冷冻（3-24 小时自选，一天一单，在冻不能再挂）。 */
    public RepairVO freeze(String me, Integer hours, String reason) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int h = hours == null ? FREEZE_MIN_HOURS : hours;
        if (h < FREEZE_MIN_HOURS || h > FREEZE_MAX_HOURS) {
            throw new BusinessException(400, "冷冻时长 " + FREEZE_MIN_HOURS + "-" + FREEZE_MAX_HOURS + " 小时，别一冻一天");
        }
        String r = reason == null ? "" : reason.trim();
        if (r.length() > REASON_MAX) {
            throw new BusinessException(400, "理由最多 " + REASON_MAX + " 字");
        }
        CoupleRepairFreeze running = freezeMapper.findCurrent(space.getId());
        if (running != null) {
            throw new BusinessException(400, "还冻着呢，先解冻再说");
        }
        long ts = System.currentTimeMillis();
        CoupleRepairFreeze row = CoupleRepairFreeze.of(space.getId(), now.toString(), me, h,
                ts + h * 3600_000L, r);
        freezeMapper.insert(row);
        push.pushCoupleEventBoth("repair-freeze", me, space.getUserA(), space.getUserB(),
                "TA 给这段架挂了冷冻，" + h + " 小时后再复温 🧊");
        return build(space, me, now);
    }

    /** 答解冻三问（冷冻提出人作答，一题一句）。 */
    public RepairVO freezeAsk(String me, String id, Integer slot, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRepairFreeze row = requireFreeze(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "三问由挂冷冻的人先答，TA 在旁边看");
        }
        if (CoupleRepairFreeze.STATUS_THAWED.equals(row.getStatus())) {
            throw new BusinessException(400, "已经解冻了，这三问留到下次用");
        }
        int s = slot == null ? 1 : slot;
        if (s < 1 || s > 3) {
            throw new BusinessException(400, "三问只有一、二、三");
        }
        String a = trim(answer, "这一问答一句，别空着");
        if (a.length() > ANSWER_MAX) {
            throw new BusinessException(400, "答案最多 " + ANSWER_MAX + " 字");
        }
        if (s == 1) {
            row.setAnswer1(a);
        } else if (s == 2) {
            row.setAnswer2(a);
        } else {
            row.setAnswer3(a);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        freezeMapper.updateById(row);
        push.pushCoupleEvent("repair-freeze-ask", me, space.partnerOf(me),
                "TA 答完了第三问里的第 " + s + " 题，等你签解冻 ✍️");
        return build(space, me, now);
    }

    /** 签解冻（到点才有效，双签 + 三问齐 = 解冻并掉修复礼盒）。 */
    public RepairVO freezeSign(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        long ts = System.currentTimeMillis();
        CoupleRepairFreeze row = requireFreeze(space, id);
        if (CoupleRepairFreeze.STATUS_THAWED.equals(row.getStatus())) {
            throw new BusinessException(400, "这单已经复温过了");
        }
        if (ts < row.getUntilAt()) {
            throw new BusinessException(400, CoupleRepairBank.freezeWaitLine((row.getUntilAt() - ts) / 60000L));
        }
        boolean isA = me.equals(space.getUserA());
        if (isA ? row.isSignedA() : row.isSignedB()) {
            return build(space, me, now);
        }
        if (isA) {
            row.setSignedA(1);
        } else {
            row.setSignedB(1);
        }
        row.setUpdatedAt(ts);
        freezeMapper.updateById(row);
        if (row.isBothSigned() && questionsDone(row)) {
            row.setStatus(CoupleRepairFreeze.STATUS_THAWED);
            freezeMapper.updateById(row);
            dropBox(space, now.toString(), row.getFromUser(), row.getId(), ts);
            dropBox(space, now.toString(), space.partnerOf(row.getFromUser()), row.getId(), ts);
            // 复温是这套系统里最难的一步：给答完三问的人记一笔心动，让「回来」有回报
            ledgerMapper.insert(CouplePointLedger.of(space.getId(), row.getFromUser(),
                    CouplePointLedger.TYPE_EARN, "复温成功：三问答完了", THAW_EARN_POINTS));
            push.pushCoupleEventBoth("repair-thawed", me, space.getUserA(), space.getUserB(),
                    CoupleRepairBank.thawedLine(CoupleRitualBank.stableHash(space.getId() + "|thaw|" + row.getId())));
        } else if (!row.isBothSigned()) {
            push.pushCoupleEvent("repair-freeze-sign", me, space.partnerOf(me),
                    questionsDone(row) ? "TA 签了解冻，三问也答完了，就等你这一笔 🧊"
                            : "TA 签了解冻，还差三问没答完 🧊");
        }
        return build(space, me, now);
    }

    private boolean questionsDone(CoupleRepairFreeze row) {
        return !row.getAnswer1().isEmpty() && !row.getAnswer2().isEmpty() && !row.getAnswer3().isEmpty();
    }

    // ========== F321 道歉质检 ==========

    /** 写道歉信并自评六要素（至少三项）。 */
    public RepairVO sorryWrite(String me, String letter, String points) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String l = trim(letter, "道歉信至少写一句人话");
        if (l.length() > LETTER_MAX) {
            throw new BusinessException(400, "道歉信最多 " + LETTER_MAX + " 字，不是论文");
        }
        List<String> ps = parsePoints(points);
        CoupleSorryReview row = CoupleSorryReview.of(space.getId(), me, l, String.join(",", ps));
        sorryMapper.insert(row);
        push.pushCoupleEvent("repair-sorry", me, space.partnerOf(me),
                "TA 交了道歉信待验货（自评 " + ps.size() + "/6 要素）📮");
        return build(space, me, now);
    }

    /** 被打回后重写（本人，仅 BACK 状态可改）。 */
    public RepairVO sorryRewrite(String me, String id, String letter, String points) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleSorryReview row = requireSorry(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "信主本人才能重写");
        }
        if (!CoupleSorryReview.STATUS_BACK.equals(row.getStatus())) {
            throw new BusinessException(400, "只有被打回的信能重写");
        }
        String l = trim(letter, "重写至少写一句");
        if (l.length() > LETTER_MAX) {
            throw new BusinessException(400, "道歉信最多 " + LETTER_MAX + " 字");
        }
        List<String> ps = parsePoints(points);
        row.setLetter(l);
        row.setPoints(String.join(",", ps));
        row.setStatus(CoupleSorryReview.STATUS_VERIFY);
        row.setVerdict("");
        row.setVerifiedBy("");
        row.setUpdatedAt(System.currentTimeMillis());
        sorryMapper.updateById(row);
        push.pushCoupleEvent("repair-sorry", me, space.partnerOf(me),
                "TA 重写了道歉信，再验一次 📮");
        return build(space, me, now);
    }

    /** 对方验货（合格进陈列室，不合格退回）。 */
    public RepairVO sorryVerify(String me, String id, Boolean pass, String verdict) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleSorryReview row = requireSorry(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己给自己验货不算数");
        }
        if (!CoupleSorryReview.STATUS_VERIFY.equals(row.getStatus())) {
            throw new BusinessException(400, "这封已经验过了");
        }
        boolean p = Boolean.TRUE.equals(pass);
        String v = verdict == null ? "" : verdict.trim();
        if (v.length() > VERDICT_MAX) {
            throw new BusinessException(400, "批注最多 " + VERDICT_MAX + " 字");
        }
        if (!p && v.isEmpty()) {
            throw new BusinessException(400, "打回要写一句差在哪");
        }
        row.setStatus(p ? CoupleSorryReview.STATUS_PASSED : CoupleSorryReview.STATUS_BACK);
        row.setVerdict(v);
        row.setVerifiedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        sorryMapper.updateById(row);
        if (p) {
            push.pushCoupleEventBoth("repair-sorry-pass", me, space.getUserA(), space.getUserB(),
                    CoupleRepairBank.verifyLine(true,
                            CoupleRitualBank.stableHash(space.getId() + "|sorry|" + row.getId())));
        } else {
            push.pushCoupleEvent("repair-sorry-back", me, row.getFromUser(),
                    CoupleRepairBank.verifyLine(false,
                            CoupleRitualBank.stableHash(space.getId() + "|sorry|" + row.getId())) + "｜" + v);
        }
        return build(space, me, now);
    }

    private List<String> parsePoints(String points) {
        List<String> out = new ArrayList<>();
        for (String raw : (points == null ? "" : points).split("[,、]")) {
            String p = raw.trim().toUpperCase();
            if (p.isEmpty()) {
                continue;
            }
            if (!CoupleSorryReview.POINTS.contains(p)) {
                throw new BusinessException(400, "要素只有 " + String.join("/", CoupleSorryReview.POINTS));
            }
            if (!out.contains(p)) {
                out.add(p);
            }
        }
        if (out.size() < POINTS_MIN) {
            throw new BusinessException(400, "六要素至少自评 " + POINTS_MIN + " 项，空口「我错了」不算道歉");
        }
        return out;
    }

    // ========== F322 重来卡 ==========

    /** 领这季的重来卡（每季一张）。 */
    public RepairVO redoApply(String me, String scene) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String quarter = quarterOf(now);
        String s = trim(scene, "要重放哪段对话得写清");
        if (s.length() > SCENE_MAX) {
            throw new BusinessException(400, "场景最多 " + SCENE_MAX + " 字");
        }
        CoupleRepairRedo row = redoMapper.findByQuarter(space.getId(), quarter);
        if (row != null && row.isUsed()) {
            throw new BusinessException(400, quarter + " 的重来卡已经用掉了，下季再来");
        }
        if (row == null) {
            redoMapper.insert(CoupleRepairRedo.of(space.getId(), quarter, s, me));
            push.pushCoupleEvent("repair-redo", me, space.partnerOf(me),
                    "TA 领了这季的重来卡：「" + s + "」，找个时间重说一遍 🔁");
        } else {
            row.setScene(s);
            row.setUpdatedAt(System.currentTimeMillis());
            redoMapper.updateById(row);
        }
        return build(space, me, now);
    }

    /** 重放完成，记下这次改说了什么。 */
    public RepairVO redoPlay(String me, String replayNote) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRepairRedo row = requireRedo(space, quarterOf(now));
        if (row.isUsed()) {
            throw new BusinessException(400, "这卡已经用过了");
        }
        String t = trim(replayNote, "这次改说了什么，写一句");
        if (t.length() > REPLAY_MAX) {
            throw new BusinessException(400, "重放记录最多 " + REPLAY_MAX + " 字");
        }
        row.setUsed(1);
        row.setReplayNote(t);
        row.setUpdatedAt(System.currentTimeMillis());
        redoMapper.updateById(row);
        push.pushCoupleEventBoth("repair-redo-played", me, space.getUserA(), space.getUserB(),
                "重来卡用掉了——" + CoupleRepairBank.redoLine(
                        CoupleRitualBank.stableHash(space.getId() + "|redo|" + row.getQuarter())));
        return build(space, me, now);
    }

    /** 给这次重放打满意度（1-5，一人一次）。 */
    public RepairVO redoRate(String me, Integer satisfaction) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRepairRedo row = requireRedo(space, quarterOf(now));
        if (!row.isUsed()) {
            throw new BusinessException(400, "还没重放，打什么分");
        }
        if (!row.getRatedBy().isEmpty()) {
            throw new BusinessException(400, "这季的重放已经打过分了");
        }
        int v = satisfaction == null ? 0 : satisfaction;
        if (v < 1 || v > 5) {
            throw new BusinessException(400, "满意度 1-5");
        }
        row.setSatisfaction(v);
        row.setRatedBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        redoMapper.updateById(row);
        push.pushCoupleEvent("repair-redo-rated", me, space.partnerOf(me),
                "TA 给这次重放打了 " + v + " 分 🔁");
        return build(space, me, now);
    }

    // ========== F323 信任重建 30 天 ==========

    /** 开重建计划（任务卡 ≤10 条，双签打卡）。 */
    public RepairVO rebuildStart(String me, String name, String cause, Integer targetDays, String tasks) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String n = trim(name, "计划叫什么");
        if (n.length() > PLAN_NAME_MAX) {
            throw new BusinessException(400, "计划名最多 " + PLAN_NAME_MAX + " 字");
        }
        if (rebuildMapper.findName(space.getId(), n) != null) {
            throw new BusinessException(400, "这个计划已经开过了");
        }
        int days = targetDays == null ? 30 : targetDays;
        if (!TARGET_DAYS.contains(days)) {
            throw new BusinessException(400, "目标天数只有 14/30 两档");
        }
        String c = cause == null ? "" : cause.trim();
        if (c.length() > REASON_MAX) {
            throw new BusinessException(400, "起因最多 " + REASON_MAX + " 字");
        }
        List<String> ts = splitTasks(tasks);
        rebuildMapper.insert(CoupleRebuildPlan.of(space.getId(), n, me, c, now.toString(), days,
                String.join(",", ts)));
        push.pushCoupleEventBoth("repair-rebuild", me, space.getUserA(), space.getUserB(),
                "开了个重建计划：「" + n + "」" + days + " 天，每天两人各签一次 🧱");
        return build(space, me, now);
    }

    /** 今天这一格我做到了（双方都签才算签到，断签可续）。 */
    public RepairVO rebuildSign(String me, String id, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRebuildPlan row = requirePlan(space, id);
        if (!CoupleRebuildPlan.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这个计划不在进行中");
        }
        String d = day == null || day.isBlank() ? now.toString() : validDay(day, "签到日");
        List<String> marks = tokens(row.getSignedDays());
        String mine = mark(d, side(space, me));
        String other = mark(d, side(space, space.partnerOf(me)));
        if (marks.contains(mine)) {
            return build(space, me, now);
        }
        marks.add(mine);
        boolean both = marks.contains(other);
        row.setSignedDays(String.join(",", marks));
        row.setUpdatedAt(System.currentTimeMillis());
        rebuildMapper.updateById(row);
        if (both) {
            int signedCount = signedCount(row.getSignedDays());
            if (signedCount >= row.getTargetDays()) {
                row.setStatus(CoupleRebuildPlan.STATUS_DONE);
                rebuildMapper.updateById(row);
                push.pushCoupleEventBoth("repair-rebuild-done", me, space.getUserA(), space.getUserB(),
                        "「" + row.getName() + "」签满 " + signedCount + " 天，信任重建达成 🏗️");
            } else {
                push.pushCoupleEventBoth("repair-rebuild-day", me, space.getUserA(), space.getUserB(),
                        "「" + row.getName() + "」今天两人都签到了（" + signedCount + "/" + row.getTargetDays() + "）🧱");
            }
        } else {
            push.pushCoupleEvent("repair-rebuild-sign", me, space.partnerOf(me),
                    "TA 已签到「" + row.getName() + "」今天这格，等你一个勾 🧱");
        }
        return build(space, me, now);
    }

    /** 周复盘（任一人可写可改）。 */
    public RepairVO rebuildReview(String me, String id, String review) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRebuildPlan row = requirePlan(space, id);
        String r = trim(review, "复盘要写一句");
        if (r.length() > REVIEW_MAX) {
            throw new BusinessException(400, "复盘最多 " + REVIEW_MAX + " 字");
        }
        row.setReview(r);
        row.setUpdatedAt(System.currentTimeMillis());
        rebuildMapper.updateById(row);
        push.pushCoupleEvent("repair-rebuild-review", me, space.partnerOf(me),
                "「" + row.getName() + "」写了周复盘，去看看 📝");
        return build(space, me, now);
    }

    /** 中止计划（开计划的人才能中止）。 */
    public RepairVO rebuildGiveup(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRebuildPlan row = requirePlan(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "谁开的计划谁才有资格中止");
        }
        if (!CoupleRebuildPlan.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这个计划已经收尾了");
        }
        row.setStatus(CoupleRebuildPlan.STATUS_GIVENUP);
        row.setUpdatedAt(System.currentTimeMillis());
        rebuildMapper.updateById(row);
        push.pushCoupleEventBoth("repair-rebuild-giveup", me, space.getUserA(), space.getUserB(),
                "「" + row.getName() + "」中止了：签过 " + signedCount(row.getSignedDays()) + " 天不清零 🧱");
        return build(space, me, now);
    }

    // ========== F324 和好了倒计时 ==========

    /** 冷战开倒计时（10-60 分钟，一天一次）。 */
    public RepairVO makeupStart(String me, Integer minutes) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int m = minutes == null ? 20 : minutes;
        if (m < MINUTE_MIN || m > MINUTE_MAX) {
            throw new BusinessException(400, "倒计时 " + MINUTE_MIN + "-" + MINUTE_MAX + " 分钟，别把冷战排班");
        }
        String day = now.toString();
        if (makeupMapper.findByDay(space.getId(), day) != null) {
            throw new BusinessException(400, "今天已经开过一次倒计时了");
        }
        long ts = System.currentTimeMillis();
        makeupMapper.insert(CoupleRepairMakeup.of(space.getId(), day, me, m, ts));
        push.pushCoupleEventBoth("repair-makeup-start", me, space.getUserA(), space.getUserB(),
                "冷战倒计时开始：" + m + " 分钟，到点自动递台阶卡 ⏳");
        return build(space, me, now);
    }

    /** 对方按暂停/继续。 */
    public RepairVO makeupPause(String me, String id, Boolean pause) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRepairMakeup row = requireMakeup(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己开的倒计时自己不能按，让 TA 缓完");
        }
        if (!CoupleRepairMakeup.STATUS_RUNNING.equals(row.getStatus())) {
            throw new BusinessException(400, "台阶已经递出去了，暂停没用了");
        }
        boolean on = Boolean.TRUE.equals(pause);
        row.setPaused(on ? 1 : 0);
        row.setPausedBy(on ? me : "");
        row.setUpdatedAt(System.currentTimeMillis());
        makeupMapper.updateById(row);
        push.pushCoupleEventBoth("repair-makeup-pause", me, space.getUserA(), space.getUserB(),
                CoupleRepairBank.pauseLine(on));
        return build(space, me, now);
    }

    /** 提前递台阶。 */
    public RepairVO makeupOffer(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRepairMakeup row = requireMakeup(space, id);
        if (!CoupleRepairMakeup.STATUS_RUNNING.equals(row.getStatus())) {
            throw new BusinessException(400, "这轮台阶已经递过了");
        }
        offer(row);
        makeupMapper.updateById(row);
        push.pushCoupleEventBoth("repair-makeup-step", me, space.getUserA(), space.getUserB(),
                "提前递台阶：" + row.getStepCard());
        return build(space, me, now);
    }

    /** 和好达成（任一方可宣布，掉修复礼盒）。 */
    public RepairVO makeupEnd(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        long ts = System.currentTimeMillis();
        CoupleRepairMakeup row = requireMakeup(space, id);
        if (CoupleRepairMakeup.STATUS_ENDED.equals(row.getStatus())) {
            return build(space, me, now);
        }
        if (CoupleRepairMakeup.STATUS_RUNNING.equals(row.getStatus())) {
            offer(row);
        }
        row.setStatus(CoupleRepairMakeup.STATUS_ENDED);
        row.setUpdatedAt(ts);
        makeupMapper.updateById(row);
        dropBox(space, now.toString(), row.getFromUser(), row.getId(), ts);
        push.pushCoupleEventBoth("repair-makeup-done", me, space.getUserA(), space.getUserB(),
                "和好了：这次倒计时 " + row.getMinutes() + " 分钟走完，礼盒掉出来了 🎁");
        return build(space, me, now);
    }

    private void offer(CoupleRepairMakeup row) {
        row.setStatus(CoupleRepairMakeup.STATUS_OFFERED);
        row.setStepCard(CoupleRepairBank.stepCard(
                CoupleRitualBank.stableHash(row.getSpaceId() + "|step|" + row.getDay() + "|" + row.getFromUser())));
    }

    // ========== F326 底线声明卡 ==========

    /** 声明/改写自己的一条底线（每人 3 格）。 */
    public RepairVO bottomSet(String me, Integer slot, String text, String sinceDay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int s = slot == null ? 1 : slot;
        if (s < 1 || s > CoupleBottomLine.SLOT_MAX) {
            throw new BusinessException(400, "底线只有 1-" + CoupleBottomLine.SLOT_MAX + " 三条，别开清单");
        }
        String t = trim(text, "底线要写一句");
        if (t.length() > LINE_TEXT_MAX) {
            throw new BusinessException(400, "底线最多 " + LINE_TEXT_MAX + " 字");
        }
        String sd = sinceDay == null || sinceDay.isBlank() ? now.toString() : validDay(sinceDay, "生效日");
        CoupleBottomLine row = bottomMapper.findBySlot(space.getId(), me, s);
        if (row == null) {
            bottomMapper.insert(CoupleBottomLine.of(space.getId(), me, s, t, sd));
            push.pushCoupleEvent("repair-bottom", me, space.partnerOf(me),
                    "TA 立了第 " + s + " 条底线：「" + t + "」，生效日 " + sd + " 🚧");
        } else {
            row.setText(t);
            row.setSinceDay(sd);
            row.setUpdatedAt(System.currentTimeMillis());
            bottomMapper.updateById(row);
        }
        return build(space, me, now);
    }

    /** 踩了对方底线要补红线记录。 */
    public RepairVO bottomBreach(String me, String id, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBottomLine row = id == null ? null : bottomMapper.findBySpace(space.getId()).stream()
                .filter(b -> b.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这条底线没登记过");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己踩自己的线不用报备，等 TA 来记");
        }
        String n = trim(note, "红线记录要写一句为什么没刹住");
        if (n.length() > BREACH_NOTE_MAX) {
            throw new BusinessException(400, "说明最多 " + BREACH_NOTE_MAX + " 字");
        }
        row.setBreachCount(row.getBreachCount() + 1);
        row.setBreachNote(n);
        row.setUpdatedAt(System.currentTimeMillis());
        bottomMapper.updateById(row);
        push.pushCoupleEvent("repair-breach", me, row.getFromUser(), CoupleRepairBank.breachLine(row.getText()));
        return build(space, me, now);
    }

    // ========== F327 我错了榜 ==========

    /** 认错（一天一次，必须写具体错在哪）。 */
    public RepairVO admit(String me, String detail) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String d = trim(detail, "错在哪要写具体一句");
        if (d.length() > DETAIL_MAX) {
            throw new BusinessException(400, "说明最多 " + DETAIL_MAX + " 字");
        }
        if (admitMapper.findByDayUser(space.getId(), day, me) != null) {
            throw new BusinessException(400, "今天已经认过一次错了，别把认错刷成打卡");
        }
        admitMapper.insert(CoupleAdmitLog.of(space.getId(), day, me, space.partnerOf(me), d));
        push.pushCoupleEvent("repair-admit", me, space.partnerOf(me), "TA 认错了：「" + d + "」🙇");
        return build(space, me, now);
    }

    /** 标「最感人的一次认错」（只有被认错的人能标）。 */
    public RepairVO admitTouch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleAdmitLog row = id == null ? null : admitMapper.findBySpace(space.getId()).stream()
                .filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这条认错不在榜上");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己给自己发感动奖不算");
        }
        if (row.isTouched()) {
            return build(space, me, now);
        }
        row.setTouched(1);
        admitMapper.updateById(row);
        push.pushCoupleEventBoth("repair-admit-touch", me, space.getUserA(), space.getUserB(),
                CoupleRepairBank.touchedLine() + "：「" + row.getDetail() + "」");
        return build(space, me, now);
    }

    // ========== F328 修复礼盒 ==========

    /** 完成补偿任务（本人）。 */
    public RepairVO boxDone(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleRepairBox row = id == null ? null : boxMapper.findBySpace(space.getId()).stream()
                .filter(b -> b.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这盒不存在");
        }
        if (!row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "盒子里的任务是 TA 的，你只能等 TA 做完");
        }
        if (CoupleRepairBox.STATUS_DONE.equals(row.getStatus())) {
            return build(space, me, now);
        }
        row.setStatus(CoupleRepairBox.STATUS_DONE);
        row.setDoneAt(System.currentTimeMillis());
        boxMapper.updateById(row);
        push.pushCoupleEventBoth("repair-box-done", me, space.getUserA(), space.getUserB(),
                CoupleRepairBank.boxDoneLine(CoupleRitualBank.stableHash(space.getId() + "|box|" + row.getId())));
        return build(space, me, now);
    }

    // ========== F329 和平纪念碑 ==========

    /** 立一句「这段吵架最代表性的话」（一天一句，本人可补注）。 */
    public RepairVO peaceSet(String me, String line, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String l = trim(line, "最代表性的那句要写出来");
        if (l.length() > PEACE_MAX) {
            throw new BusinessException(400, "那句话最多 " + PEACE_MAX + " 字");
        }
        String n = note == null ? "" : note.trim();
        if (n.length() > PEACE_NOTE_MAX) {
            throw new BusinessException(400, "回看注解最多 " + PEACE_NOTE_MAX + " 字");
        }
        CouplePeaceLine row = peaceMapper.findBySpace(space.getId()).stream()
                .filter(p -> p.getDay().equals(day) && p.getFromUser().equals(me)).findFirst().orElse(null);
        if (row == null) {
            peaceMapper.insert(CouplePeaceLine.of(space.getId(), day, me, l, n));
            push.pushCoupleEvent("repair-peace", me, space.partnerOf(me),
                    "纪念碑上新刻了一句：「" + l + "」🗿");
        } else {
            row.setNote(n);
            peaceMapper.updateById(row);
        }
        return build(space, me, now);
    }

    // ========== 惰性结算 ==========

    private void settle(CoupleSpace space, long ts) {
        for (CoupleRepairMakeup m : makeupMapper.findRunning(space.getId())) {
            if (m.endAt() <= ts && !m.isPaused()) {
                offer(m);
                m.setUpdatedAt(ts);
                makeupMapper.updateById(m);
                push.pushCoupleEventBoth("repair-makeup-step", m.getFromUser(), space.getUserA(), space.getUserB(),
                        "时间到了，台阶自动递上：" + m.getStepCard());
            }
        }
    }

    // ========== 聚合 ==========

    private RepairVO build(CoupleSpace space, String me, LocalDate now) {
        String today = now.toString();
        long ts = System.currentTimeMillis();
        String quarter = quarterOf(now);
        String year = String.valueOf(now.getYear());

        List<FreezeVO> freezes = new ArrayList<>();
        FreezeVO current = null;
        int freezeKept = 0;
        for (CoupleRepairFreeze f : freezeMapper.findBySpace(space.getId())) {
            if (freezeKept++ >= LIST_FREEZE_MAX) {
                break;
            }
            boolean mine = me.equals(f.getFromUser());
            boolean signedMe = me.equals(space.getUserA()) ? f.isSignedA() : f.isSignedB();
            boolean signedPartner = me.equals(space.getUserA()) ? f.isSignedB() : f.isSignedA();
            FreezeVO vo = new FreezeVO(f.getId(), f.getStartDay(), mine, f.getHours(), f.getStatus(),
                    Math.max(0, (f.getUntilAt() - ts) / 60000L), f.getReason(), f.getAnswer1(), f.getAnswer2(),
                    f.getAnswer3(), signedMe, signedPartner, f.isBothSigned(), questionsDone(f),
                    !CoupleRepairFreeze.STATUS_THAWED.equals(f.getStatus()) && !signedMe);
            freezes.add(vo);
            if (current == null && CoupleRepairFreeze.STATUS_FROZEN.equals(f.getStatus())) {
                current = vo;
            }
        }

        List<SorryVO> sorries = new ArrayList<>();
        int sorryKept = 0;
        for (CoupleSorryReview s : sorryMapper.findBySpace(space.getId())) {
            if (sorryKept++ >= LIST_SORRY_MAX) {
                break;
            }
            sorries.add(new SorryVO(s.getId(), me.equals(s.getFromUser()), s.getLetter(), tokens(s.getPoints()),
                    s.getStatus(), s.getVerdict(), s.getVerifiedBy(), !me.equals(s.getFromUser())
                    && CoupleSorryReview.STATUS_VERIFY.equals(s.getStatus())));
        }

        CoupleRepairRedo redoRow = redoMapper.findByQuarter(space.getId(), quarter);
        RedoVO redoVO = redoRow == null
                ? new RedoVO(quarter, false, "", false, "", null, "", true, false)
                : new RedoVO(quarter, me.equals(redoRow.getFromUser()), redoRow.getScene(), redoRow.isUsed(),
                redoRow.getReplayNote(), redoRow.getSatisfaction(), redoRow.getRatedBy(),
                !redoRow.isUsed(), redoRow.isUsed() && redoRow.getRatedBy().isEmpty());

        List<RebuildVO> rebuilds = new ArrayList<>();
        int planKept = 0;
        for (CoupleRebuildPlan p : rebuildMapper.findBySpace(space.getId())) {
            if (planKept++ >= LIST_PLAN_MAX) {
                break;
            }
            List<TaskVO> tasks = new ArrayList<>();
            List<String> rawTasks = tokens(p.getTasks());
            for (int i = 0; i < rawTasks.size(); i++) {
                tasks.add(new TaskVO(i + 1, rawTasks.get(i)));
            }
            int dayNo = (int) Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.parse(p.getStartDay()), now) + 1);
            rebuilds.add(new RebuildVO(p.getId(), p.getName(), me.equals(p.getFromUser()), p.getCause(),
                    p.getStartDay(), p.getTargetDays(), tasks, signedCount(p.getSignedDays()),
                    Math.min(dayNo, p.getTargetDays()), p.getReview(), p.getStatus()));
        }

        List<MakeupVO> makeups = new ArrayList<>();
        MakeupVO makeupVO = null;
        for (CoupleRepairMakeup m : makeupMapper.findBySpace(space.getId())) {
            boolean mine = me.equals(m.getFromUser());
            MakeupVO vo = new MakeupVO(m.getId(), m.getDay(), mine, m.getMinutes(), m.getStatus(),
                    Math.max(0, (m.endAt() - ts) / 1000L), m.isPaused(), m.getPausedBy(), m.getStepCard(),
                    !mine && CoupleRepairMakeup.STATUS_RUNNING.equals(m.getStatus()),
                    CoupleRepairMakeup.STATUS_RUNNING.equals(m.getStatus())
                            || CoupleRepairMakeup.STATUS_OFFERED.equals(m.getStatus()));
            makeups.add(vo);
            if (m.getDay().equals(today)) {
                makeupVO = vo;
            }
        }

        List<BottomVO> bottoms = new ArrayList<>();
        for (CoupleBottomLine b : bottomMapper.findBySpace(space.getId())) {
            bottoms.add(new BottomVO(b.getId(), me.equals(b.getFromUser()), b.getSlot(), b.getText(),
                    b.getSinceDay(), b.getBreachCount(), b.getBreachNote(), !me.equals(b.getFromUser())));
        }

        List<AdmitVO> admits = new ArrayList<>();
        int admitKept = 0;
        for (CoupleAdmitLog a : admitMapper.findBySpace(space.getId())) {
            if (admitKept++ >= LIST_ADMIT_MAX) {
                break;
            }
            admits.add(new AdmitVO(a.getId(), a.getDay(), me.equals(a.getFromUser()), a.getAboutUser(),
                    a.getDetail(), a.isTouched(), !me.equals(a.getFromUser()) && !a.isTouched()));
        }

        List<BoxVO> boxes = new ArrayList<>();
        int boxKept = 0;
        for (CoupleRepairBox b : boxMapper.findBySpace(space.getId())) {
            if (boxKept++ >= LIST_BOX_MAX) {
                break;
            }
            boxes.add(new BoxVO(b.getId(), b.getDay(), me.equals(b.getOwnerUser()), b.getTask(), b.getStatus(),
                    CoupleRepairBox.STATUS_DONE.equals(b.getStatus()) ? CoupleRepairBank.boxDoneLine(
                            CoupleRitualBank.stableHash(space.getId() + "|box|" + b.getId())) : ""));
        }

        List<PeaceVO> peace = new ArrayList<>();
        int peaceKept = 0;
        for (CouplePeaceLine p : peaceMapper.findBySpace(space.getId())) {
            if (peaceKept++ >= LIST_PEACE_MAX) {
                break;
            }
            peace.add(new PeaceVO(p.getId(), p.getDay(), me.equals(p.getFromUser()), p.getLine(), p.getNote()));
        }

        ReportVO report = report(space, now);

        return new RepairVO(today, quarter, year, freezes, current, sorries, redoVO, rebuilds, makeupVO,
                makeups, bottoms, admits, boxes, peace, report);
    }

    /** F325 冲突类型年报（读时聚合，无表；全部走原始表计数，不受列表钳制影响）。 */
    private ReportVO report(CoupleSpace space, LocalDate now) {
        String yearPrefix = String.valueOf(now.getYear());
        int fz = 0;
        int thawed = 0;
        int hours = 0;
        for (CoupleRepairFreeze f : freezeMapper.findBySpace(space.getId())) {
            if (!f.getStartDay().startsWith(String.valueOf(now.getYear()))) {
                continue;
            }
            fz++;
            hours += f.getHours();
            if (CoupleRepairFreeze.STATUS_THAWED.equals(f.getStatus())) {
                thawed++;
            }
        }
        int in = 0;
        int passed = 0;
        int backed = 0;
        for (CoupleSorryReview s : sorryMapper.findBySpace(space.getId())) {
            if (CoupleSorryReview.STATUS_VERIFY.equals(s.getStatus())) {
                in++;
            } else if (CoupleSorryReview.STATUS_PASSED.equals(s.getStatus())) {
                passed++;
            } else {
                backed++;
            }
        }
        List<CoupleAdmitLog> admitRows = admitMapper.findBySpace(space.getId());
        int admitCount = (int) admitRows.stream().filter(a -> a.getDay().startsWith(yearPrefix)).count();
        int touched = (int) admitRows.stream().filter(a -> a.isTouched() && a.getDay().startsWith(yearPrefix)).count();
        int boxesDone = (int) boxMapper.findBySpace(space.getId()).stream()
                .filter(b -> CoupleRepairBox.STATUS_DONE.equals(b.getStatus())).count();
        int redoUsed = (int) redoMapper.findBySpace(space.getId()).stream()
                .filter(CoupleRepairRedo::isUsed).count();
        int peaceLines = (int) peaceMapper.findBySpace(space.getId()).stream()
                .filter(p -> p.getDay().startsWith(yearPrefix)).count();
        int reconciles = 0;
        int accepted = 0;
        for (CoupleReconcile r : reconcileMapper.findBySpace(space.getId())) {
            if (r.getStartAt() == null || java.time.LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(r.getStartAt()), java.time.ZoneId.systemDefault())
                    .getYear() != now.getYear()) {
                continue;
            }
            reconciles++;
            if (r.getAcceptedBy() != null && !r.getAcceptedBy().isEmpty()) {
                accepted++;
            }
        }
        long seed = CoupleRitualBank.stableHash(space.getId() + "|report|" + now.getYear());
        return new ReportVO(String.valueOf(now.getYear()), fz, thawed, in, passed, backed, admitCount, touched,
                redoUsed, boxesDone, peaceLines, reconciles, accepted,
                fz == 0 ? 0 : Math.round((float) hours / fz),
                CoupleRepairBank.reportPrize(seed, admitCount, passed),
                reconciles == 0 ? CoupleRepairBank.reportSummary(seed, fz, thawed)
                        : CoupleRepairBank.reportSummary(seed, fz, thawed)
                                + "另外写过 " + reconciles + " 份矛盾复盘，" + accepted + " 份被对方接住。");
    }

    // ========== 小件 ==========

    private void dropBox(CoupleSpace space, String day, String owner, String fromId, long ts) {
        if (boxMapper.findByDayUser(space.getId(), day, owner) != null) {
            return;
        }
        String task = CoupleRepairBank.boxTask(
                CoupleRitualBank.stableHash(space.getId() + "|box|" + day + "|" + owner));
        boxMapper.insert(CoupleRepairBox.of(space.getId(), day, owner,
                task.length() > BOX_TASK_MAX ? task.substring(0, BOX_TASK_MAX) : task,
                fromId == null ? "" : fromId));
    }

    private String side(CoupleSpace space, String user) {
        return user.equals(space.getUserA()) ? "A" : "B";
    }

    /** 双方都签到的天数（记号 MMdd:A 与 MMdd:B 成对才算一天）。 */
    private int signedCount(String signedDays) {
        Set<String> seen = new LinkedHashSet<>(tokens(signedDays));
        int out = 0;
        for (String token : seen) {
            int i = token.indexOf(':');
            if (i > 0 && token.endsWith(":A") && seen.contains(token.substring(0, i) + ":B")) {
                out++;
            }
        }
        return out;
    }

    /** 签到记号：MMdd:A / MMdd:B（一期内不跨年重复，列宽容得下 30 天双签）。 */
    private String mark(String day, String side) {
        return day.replace("-", "").substring(4) + ":" + side;
    }

    private List<String> tokens(String csv) {
        List<String> out = new ArrayList<>();
        for (String raw : (csv == null ? "" : csv).split(",")) {
            String t = raw.trim();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private List<String> splitTasks(String tasks) {
        List<String> out = new ArrayList<>();
        for (String raw : (tasks == null ? "" : tasks).split("[,、\n]")) {
            String t = raw.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > TASK_MAX) {
                throw new BusinessException(400, "一条任务最多 " + TASK_MAX + " 字");
            }
            if (!out.contains(t)) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            throw new BusinessException(400, "任务卡至少一条，光立计划不干活没用");
        }
        if (out.size() > TASK_LIMIT) {
            throw new BusinessException(400, "任务卡最多 " + TASK_LIMIT + " 条，先把眼前的做完");
        }
        return out;
    }

    private String quarterOf(LocalDate now) {
        return now.getYear() + "Q" + ((now.getMonthValue() - 1) / 3 + 1);
    }

    private CoupleRepairFreeze requireFreeze(CoupleSpace space, String id) {
        CoupleRepairFreeze row = id == null ? null : freezeMapper.findBySpace(space.getId()).stream()
                .filter(f -> f.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这单冷冻不存在");
        }
        return row;
    }

    private CoupleSorryReview requireSorry(CoupleSpace space, String id) {
        CoupleSorryReview row = id == null ? null : sorryMapper.findBySpace(space.getId()).stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这封道歉信不在架上");
        }
        return row;
    }

    private CoupleRepairRedo requireRedo(CoupleSpace space, String quarter) {
        CoupleRepairRedo row = redoMapper.findByQuarter(space.getId(), quarter);
        if (row == null) {
            throw new BusinessException(400, "这季的重来卡还没领");
        }
        return row;
    }

    private CoupleRebuildPlan requirePlan(CoupleSpace space, String id) {
        CoupleRebuildPlan row = id == null ? null : rebuildMapper.findBySpace(space.getId()).stream()
                .filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这个重建计划不存在");
        }
        return row;
    }

    private CoupleRepairMakeup requireMakeup(CoupleSpace space, String id) {
        CoupleRepairMakeup row = id == null ? null : makeupMapper.findBySpace(space.getId()).stream()
                .filter(m -> m.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这轮倒计时不存在");
        }
        return row;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private String validDay(String v, String label) {
        try {
            return LocalDate.parse(v.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, label + "要写成 yyyy-MM-dd");
        }
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
