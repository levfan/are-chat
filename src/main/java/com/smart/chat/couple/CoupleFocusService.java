package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * 注意力保护区（F360-F369，批次三十二）：专注打卡、专属时段、攒一句话、饭桌不低头、对视十秒、
 * 不插电半小时、走神温柔哨、专注周报、数字排毒半天、注意力年报。
 * 情绪命题：注意力是当代最贵的礼物，「我在看你」比「我爱你」稀缺——
 * 把不被手机抢走的注意力记成账，双方都记的那一天才算真的发生过。
 * 双人列口径：_a 属 couple_space 的 userA，_b 属 userB，读自己那一列由 mine 决定；连击/点亮一律读时算，不建定时任务。
 */
@Service
public class CoupleFocusService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleFocusNightMapper nightMapper;
    private final CoupleFocusSlotMapper slotMapper;
    private final CoupleFocusQueueMapper queueMapper;
    private final CoupleFocusMealMapper mealMapper;
    private final CoupleFocusGazeMapper gazeMapper;
    private final CoupleFocusUnplugMapper unplugMapper;
    private final CoupleFocusNudgeMapper nudgeMapper;
    private final CoupleFocusDetoxMapper detoxMapper;
    private final ImPushService push;

    public CoupleFocusService(CoupleSpaceMapper spaceMapper, CoupleFocusNightMapper nightMapper,
                              CoupleFocusSlotMapper slotMapper, CoupleFocusQueueMapper queueMapper,
                              CoupleFocusMealMapper mealMapper, CoupleFocusGazeMapper gazeMapper,
                              CoupleFocusUnplugMapper unplugMapper, CoupleFocusNudgeMapper nudgeMapper,
                              CoupleFocusDetoxMapper detoxMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.nightMapper = nightMapper;
        this.slotMapper = slotMapper;
        this.queueMapper = queueMapper;
        this.mealMapper = mealMapper;
        this.gazeMapper = gazeMapper;
        this.unplugMapper = unplugMapper;
        this.nudgeMapper = nudgeMapper;
        this.detoxMapper = detoxMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F360 当夜专注打卡（mineMinutes/partnerMinutes 为 null 表示那一方还没报；bothLit 读时算）。 */
    public record NightVO(boolean mineReported, boolean partnerReported, Integer mineMinutes,
                          Integer partnerMinutes, String mineNote, String partnerNote, boolean bothLit,
                          int totalMinutes, String hint) {
    }

    /** F362 攒下来的一句话。 */
    public record QueueVO(String id, String fromUser, boolean mine, String content, boolean read, long created) {
    }

    /** F361 本周专属时段（confirmed=对方已点头）。 */
    public record SlotVO(String id, String week, String day, String title, int hours, String proposedBy,
                         boolean mine, boolean confirmed, long created) {
    }

    /** F367 专注周报（周一锚，数字全部来自真实表）。 */
    public record WeeklyVO(String week, String fromDay, String toDay, int minutes, int litNights, int meals,
                           int gazes, int unplugs, int slots, int nudges, int unplugStreak, String summary) {
    }

    /** F369 注意力年报（hours=为彼此放下的手机小时数）。 */
    public record YearlyVO(int year, int minutes, String hours, int litNights, int meals, int gazes,
                           int unplugs, int detox, String topDay, int topMinutes, String summary) {
    }

    /** 今日注意力总览：写接口一律原样返回这份聚合，前端整体替换。 */
    public record TodayVO(String day, NightVO night, int queueUnread, List<QueueVO> queue, SlotVO slot,
                          int meals, boolean mealMine, boolean mealBoth,
                          int gazes, boolean gazeMine, boolean gazeBoth, boolean unplugMine,
                          boolean unplugBoth, int unplugStreak, int nudgesToday, int nudgeQuotaLeft,
                          boolean detoxMine, boolean detoxBoth, String detoxKind) {
    }

    // ========== 读 ==========

    /**
     * 今日注意力总览（GET /today）。
     * ⚠️ 这里**不能**顺手签收：读时置已读会让 queueUnread 恒 0、`POST /queue/read` 恒签 0 条、
     * `focus-queue-read` 回执永远不推——一打开页面就把「一键收全部」这个功能吃掉了。签收只在 queueRead 里做。
     */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        return build(space, me, now);
    }

    // ========== F360 专注打卡 ==========

    /** F360 报今晚放下的分钟数（0-180 钳制；自己那列可改写；先报提示对方、双报当夜点亮）。 */
    public TodayVO night(String me, Integer minutes, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        int v = minutes == null ? CoupleFocusNight.MINUTES_MIN
                : Math.max(CoupleFocusNight.MINUTES_MIN, Math.min(CoupleFocusNight.MINUTES_MAX, minutes));
        String text = note == null ? "" : note.trim();
        if (text.length() > CoupleFocusNight.NOTE_MAX) {
            throw new BusinessException(400, "一句话最多 " + CoupleFocusNight.NOTE_MAX + " 字");
        }
        boolean isA = space.getUserA().equals(me);
        CoupleFocusNight row = nightMapper.findByDay(space.getId(), day);
        if (row == null) {
            row = CoupleFocusNight.of(space.getId(), day);
            row.report(isA, v, text);
            nightMapper.insert(row);
        } else {
            row.report(isA, v, text);
            nightMapper.updateById(row);
        }
        boolean partnerReported = row.reported(!isA);
        if (partnerReported) {
            push.pushCoupleEventBoth("focus-night-lit", me, space.getUserA(), space.getUserB(),
                    CoupleFocusBank.nightLitLine(String.valueOf(v), (int) row.totalMinutes()));
        } else {
            push.pushCoupleEventBoth("focus-night-reported", me, space.getUserA(), space.getUserB(),
                    CoupleFocusBank.nightOneSideLine(String.valueOf(v)));
        }
        return build(space, me, now);
    }

    // ========== F361 专属时段 ==========

    /** F361 预约本周「只属于我们」的时段（title ≤60、hours 1-6 钳制、day 必须在本周内；推对方等确认）。 */
    public TodayVO proposeSlot(String me, String title, Integer hours, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String text = trim(title, "总得写点什么，这段时间做什么");
        if (text.length() > CoupleFocusSlot.TITLE_MAX) {
            throw new BusinessException(400, "「做什么」最多 " + CoupleFocusSlot.TITLE_MAX + " 字");
        }
        LocalDate weekStart = weekStart(now);
        LocalDate d = parseDay(day, "哪天");
        if (d.isBefore(weekStart) || d.isAfter(weekStart.plusDays(6))) {
            throw new BusinessException(400, "时段要落在本周（" + weekStart + " ~ " + weekStart.plusDays(6) + "）");
        }
        int h = hours == null ? CoupleFocusSlot.HOURS_DEFAULT
                : Math.max(CoupleFocusSlot.HOURS_MIN, Math.min(CoupleFocusSlot.HOURS_MAX, hours));
        String week = weekStart.toString();
        CoupleFocusSlot row = slotMapper.findByWeek(space.getId(), week);
        if (row == null) {
            row = CoupleFocusSlot.of(space.getId(), week, d.toString(), text, h, me);
            slotMapper.insert(row);
        } else {
            row.setDay(d.toString());
            row.setTitle(text);
            row.setHours(h);
            row.setProposedBy(me);
            row.setConfirmer(null);
            row.setUpdatedAt(System.currentTimeMillis());
            slotMapper.updateById(row);
        }
        push.pushCoupleEvent("focus-slot-planned", me, space.partnerOf(me),
                CoupleFocusBank.slotPlannedLine(text, d.toString(), h));
        return build(space, me, now);
    }

    /** F361 对方确认（自己写的自己确认不算，400）；确认后推双方。 */
    public TodayVO confirmSlot(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleFocusSlot row = slotMapper.findByWeek(space.getId(), weekStart(now).toString());
        if (row == null) {
            throw new BusinessException(400, "这周还没有人预约专属时段");
        }
        if (me.equals(row.getProposedBy())) {
            throw new BusinessException(400, "自己写的时段不能自己确认，等 TA 点头");
        }
        if (row.confirmed()) {
            return build(space, me, now);
        }
        row.setConfirmer(me);
        row.setUpdatedAt(System.currentTimeMillis());
        slotMapper.updateById(row);
        push.pushCoupleEventBoth("focus-slot-confirmed", me, space.getUserA(), space.getUserB(),
                CoupleFocusBank.slotConfirmedLine(nz(row.getTitle()), nz(row.getDay())));
        return build(space, me, now);
    }

    // ========== F362 攒一句话 ==========

    /** F362 把一句话攒进队列（≤80 字，在途每人 ≤5，超出 400；推对方）。 */
    public TodayVO queueAdd(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String text = trim(content, "想说的那句话说一句");
        if (text.length() > CoupleFocusQueue.CONTENT_MAX) {
            throw new BusinessException(400, "一句话最多 " + CoupleFocusQueue.CONTENT_MAX + " 字");
        }
        long inFlight = queueMapper.findByFromUser(space.getId(), me).stream()
                .filter(q -> !q.read()).count();
        if (inFlight >= CoupleFocusQueue.IN_FLIGHT_MAX) {
            throw new BusinessException(400, "攒了 " + CoupleFocusQueue.IN_FLIGHT_MAX + " 句了，等 TA 收一下吧");
        }
        queueMapper.insert(CoupleFocusQueue.of(space.getId(), me, space.partnerOf(me), text));
        push.pushCoupleEvent("focus-queue-added", me, space.partnerOf(me),
                CoupleFocusBank.queueAddedLine(text));
        return build(space, me, now);
    }

    /** F362 一键收全部（推回执 focus-queue-read，count 只算本次真的置成已读的）。 */
    public TodayVO queueRead(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        int count = settleRead(space, me);
        if (count > 0) {
            push.pushCoupleEventBoth("focus-queue-read", me, space.getUserA(), space.getUserB(),
                    CoupleFocusBank.queueReadLine(me, count));
        }
        return build(space, me, now);
    }

    /** F362 批量签收 to_user=me 的未读留言（返回本次真的置成已读的条数；GET 读时结算不推事件）。 */
    private int settleRead(CoupleSpace space, String me) {
        int count = 0;
        for (CoupleFocusQueue row : queueMapper.findByToUser(space.getId(), me)) {
            if (row.read()) {
                continue;
            }
            row.markRead();
            queueMapper.updateById(row);
            count++;
        }
        return count;
    }

    // ========== F363 饭桌不低头 / F364 对视十秒 / F365 不插电半小时 ==========

    /** F363 吃饭手机倒扣打卡（双方各点各的，双点=同桌成功推双方）。 */
    public TodayVO mealTick(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        boolean isA = space.getUserA().equals(me);
        CoupleFocusMeal row = mealMapper.findByDay(space.getId(), day);
        if (row == null) {
            row = CoupleFocusMeal.of(space.getId(), day);
            mealMapper.insert(row);
        }
        if (row.tick(isA)) {
            mealMapper.updateById(row);
            if (row.bothTicked()) {
                push.pushCoupleEventBoth("focus-meal-both", me, space.getUserA(), space.getUserB(),
                        CoupleFocusBank.mealBothLine());
            }
        }
        return build(space, me, now);
    }

    /** F364 对视十秒打卡（双点点亮推双方）。 */
    public TodayVO gazeTick(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        boolean isA = space.getUserA().equals(me);
        CoupleFocusGaze row = gazeMapper.findByDay(space.getId(), day);
        if (row == null) {
            row = CoupleFocusGaze.of(space.getId(), day);
            gazeMapper.insert(row);
        }
        if (row.tick(isA)) {
            gazeMapper.updateById(row);
            if (row.bothTicked()) {
                push.pushCoupleEventBoth("focus-gaze-both", me, space.getUserA(), space.getUserB(),
                        CoupleFocusBank.gazeBothLine());
            }
        }
        return build(space, me, now);
    }

    /** F365 不插电半小时打卡（双点成功推双方；周连击读时算）。 */
    public TodayVO unplugTick(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        boolean isA = space.getUserA().equals(me);
        CoupleFocusUnplug row = unplugMapper.findByDay(space.getId(), day);
        if (row == null) {
            row = CoupleFocusUnplug.of(space.getId(), day);
            unplugMapper.insert(row);
        }
        if (row.tick(isA)) {
            unplugMapper.updateById(row);
            if (row.bothTicked()) {
                push.pushCoupleEventBoth("focus-unplug-both", me, space.getUserA(), space.getUserB(),
                        CoupleFocusBank.unplugBothLine());
            }
        }
        return build(space, me, now);
    }

    // ========== F366 走神温柔哨 ==========

    /** F366 递一张「回来啦」卡（每人每天 ≤2 张，超出 400；note 可空 ≤40 字；只推收卡人）。 */
    public TodayVO nudge(String me, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String text = note == null ? "" : note.trim();
        if (text.length() > CoupleFocusNudge.NOTE_MAX) {
            throw new BusinessException(400, "哨子上的话最多 " + CoupleFocusNudge.NOTE_MAX + " 字");
        }
        if (nudgeMapper.findByDayUser(space.getId(), day, me).size() >= CoupleFocusNudge.DAILY_MAX) {
            throw new BusinessException(400, "今天的 " + CoupleFocusNudge.DAILY_MAX + " 张哨卡都用完了，再吹就唠叨了");
        }
        nudgeMapper.insert(CoupleFocusNudge.of(space.getId(), day, me, text));
        long seed = CoupleRitualBank.stableHash(space.getId() + "|focus-nudge|" + day);
        push.pushCoupleEvent("focus-nudge-sent", me, space.partnerOf(me),
                CoupleFocusBank.nudgeLine(seed, text));
        return build(space, me, now);
    }

    // ========== F368 数字排毒半天 ==========

    /** F368 发起/应战半日无手机挑战（kind 只能 AM/PM；一天一格；双报达成「清净半天」）。 */
    public TodayVO detox(String me, String kind) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String k = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleFocusDetox.KIND_AM.equals(k) && !CoupleFocusDetox.KIND_PM.equals(k)) {
            throw new BusinessException(400, "只能选 AM 或 PM");
        }
        boolean isA = space.getUserA().equals(me);
        CoupleFocusDetox row = detoxMapper.findByDay(space.getId(), day);
        boolean anyoneYet;
        if (row == null) {
            row = CoupleFocusDetox.of(space.getId(), day, k);
            anyoneYet = false;
            detoxMapper.insert(row);
        } else {
            anyoneYet = row.ticked(true) || row.ticked(false);
        }
        boolean changed = row.tick(isA, me);
        if (changed) {
            detoxMapper.updateById(row);
        }
        if (changed && row.bothTicked()) {
            push.pushCoupleEventBoth("focus-detox-done", me, space.getUserA(), space.getUserB(),
                    CoupleFocusBank.detoxDoneLine(CoupleFocusBank.kindLabel(row.getKind()), day));
        } else if (changed && !anyoneYet) {
            push.pushCoupleEvent("focus-detox-started", me, space.partnerOf(me),
                    CoupleFocusBank.detoxStartedLine(CoupleFocusBank.kindLabel(row.getKind()), day));
        }
        return build(space, me, now);
    }

    // ========== F367 专注周报 ==========

    /** F367 专注周报（周一锚：本周专注分钟/同桌/对视/不插电/时段/哨卡 + Bank 文案）。 */
    public WeeklyVO weekly(String me) {
        CoupleSpace space = requireSpace(me);
        return weeklyOf(space, LocalDate.now());
    }

    private WeeklyVO weeklyOf(CoupleSpace space, LocalDate now) {
        LocalDate from = weekStart(now);
        LocalDate to = from.plusDays(6);
        String fromDay = from.toString();
        String toDay = to.toString();
        String week = fromDay;
        int minutes = 0;
        for (CoupleFocusNight n : nightMapper.findByDayRange(space.getId(), fromDay, toDay)) {
            minutes += n.totalMinutes();
        }
        int meals = (int) mealMapper.findBySpace(space.getId()).stream()
                .filter(m -> inRange(m.getDay(), fromDay, toDay) && m.bothTicked()).count();
        int gazes = (int) gazeMapper.findByYear(space.getId(), String.valueOf(now.getYear())).stream()
                .filter(g -> inRange(g.getDay(), fromDay, toDay) && g.bothTicked()).count();
        int unplugs = (int) unplugMapper.findByYear(space.getId(), String.valueOf(now.getYear())).stream()
                .filter(u -> inRange(u.getDay(), fromDay, toDay) && u.bothTicked()).count();
        int slots = (int) slotMapper.findBySpace(space.getId()).stream()
                .filter(s -> week.equals(s.getWeek()) && s.confirmed()).count();
        int nudges = (int) nudgeMapper.findByYear(space.getId(), String.valueOf(now.getYear())).stream()
                .filter(n -> inRange(n.getDay(), fromDay, toDay)).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|focus-week|" + week);
        return new WeeklyVO(week, fromDay, toDay, minutes, litNights(space, fromDay, toDay), meals, gazes,
                unplugs, slots, nudges, streak(space, now),
                CoupleFocusBank.weekSummary(week, fromDay, toDay, minutes, meals, gazes, unplugs, slots,
                        nudges, seed));
    }

    private int litNights(CoupleSpace space, String fromDay, String toDay) {
        return (int) nightMapper.findByDayRange(space.getId(), fromDay, toDay).stream()
                .filter(CoupleFocusNight::bothLit).count();
    }

    // ========== F369 注意力年报 ==========

    /** F369 注意力年报（为彼此放下的手机小时数 + 最专注的一天；year 缺省当年）。 */
    public YearlyVO yearReport(String me, String year) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        return yearly(space, normalizeYear(year, now));
    }

    private YearlyVO yearly(CoupleSpace space, String y) {
        List<CoupleFocusNight> nights = nightMapper.findByYear(space.getId(), y);
        int minutes = 0;
        int lit = 0;
        String topDay = "";
        int topMinutes = 0;
        for (CoupleFocusNight n : nights) {
            minutes += n.totalMinutes();
            if (n.bothLit()) {
                lit++;
            }
            if ((int) n.totalMinutes() > topMinutes) {
                topMinutes = (int) n.totalMinutes();
                topDay = nz(n.getDay());
            }
        }
        int meals = (int) mealMapper.findBySpace(space.getId()).stream()
                .filter(m -> yearOf(m.getDay()) == Integer.parseInt(y) && m.bothTicked()).count();
        List<CoupleFocusGaze> gazes = gazeMapper.findByYear(space.getId(), y);
        int gazeCount = (int) gazes.stream().filter(CoupleFocusGaze::bothTicked).count();
        List<CoupleFocusUnplug> unplugs = unplugMapper.findByYear(space.getId(), y);
        int unplugCount = (int) unplugs.stream().filter(CoupleFocusUnplug::bothTicked).count();
        int detox = (int) detoxMapper.findBySpace(space.getId()).stream()
                .filter(d -> yearOf(d.getDay()) == Integer.parseInt(y) && d.bothTicked()).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|focus-year|" + y);
        String hours = String.format(Locale.ROOT, "%.1f", minutes / 60.0);
        return new YearlyVO(Integer.parseInt(y), minutes, hours, lit, meals, gazeCount, unplugCount, detox,
                topDay, topMinutes,
                CoupleFocusBank.yearSummary(y, minutes, hours, lit, meals, gazeCount, unplugCount, detox,
                        topDay, topMinutes, seed));
    }

    // ========== 聚合 ==========

    private TodayVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        boolean isA = space.getUserA().equals(me);
        CoupleFocusNight night = nightMapper.findByDay(space.getId(), day);

        List<QueueVO> queue = queueMapper.findByToUser(space.getId(), me).stream()
                .map(q -> new QueueVO(q.getId(), nz(q.getFromUser()), q.getFromUser().equals(me),
                        nz(q.getContent()), q.read(), q.getCreated() == null ? 0 : q.getCreated()))
                .toList();
        int unread = (int) queue.stream().filter(q -> !q.read()).count();

        CoupleFocusSlot slotRow = slotMapper.findByWeek(space.getId(), weekStart(now).toString());
        SlotVO slot = slotRow == null ? null : new SlotVO(slotRow.getId(), nz(slotRow.getWeek()),
                nz(slotRow.getDay()), nz(slotRow.getTitle()), slotRow.hoursOrDefault(),
                nz(slotRow.getProposedBy()), me.equals(slotRow.getProposedBy()), slotRow.confirmed(),
                slotRow.getCreated() == null ? 0 : slotRow.getCreated());

        CoupleFocusMeal meal = mealMapper.findByDay(space.getId(), day);
        CoupleFocusGaze gaze = gazeMapper.findByDay(space.getId(), day);
        CoupleFocusUnplug unplug = unplugMapper.findByDay(space.getId(), day);
        int nudgesToday = nudgeMapper.findByDay(space.getId(), day).size();
        CoupleFocusDetox detox = detoxMapper.findByDay(space.getId(), day);

        return new TodayVO(day, toNight(night, isA),
                unread, queue, slot,
                meal == null ? 0 : (meal.ticked(true) ? 1 : 0) + (meal.ticked(false) ? 1 : 0),
                meal != null && meal.ticked(isA),
                meal != null && meal.bothTicked(),
                gaze == null ? 0 : (gaze.ticked(true) ? 1 : 0) + (gaze.ticked(false) ? 1 : 0),
                gaze != null && gaze.ticked(isA),
                gaze != null && gaze.bothTicked(),
                unplug != null && unplug.ticked(isA),
                unplug != null && unplug.bothTicked(),
                streak(space, now),
                nudgesToday,
                Math.max(0, CoupleFocusNudge.DAILY_MAX
                        - nudgeMapper.findByDayUser(space.getId(), day, me).size()),
                detox != null && detox.ticked(isA),
                detox != null && detox.bothTicked(),
                detox == null ? null : nz(detox.getKind()));
    }

    private NightVO toNight(CoupleFocusNight row, boolean isA) {
        if (row == null) {
            return new NightVO(false, false, null, null, "", "", false, 0, "");
        }
        Integer mineMinutes = isA ? row.getMinutesA() : row.getMinutesB();
        Integer partnerMinutes = isA ? row.getMinutesB() : row.getMinutesA();
        boolean both = row.bothLit();
        String hint = both ? "" : CoupleFocusBank.nightWaitingLine();
        return new NightVO(mineMinutes != null, partnerMinutes != null, mineMinutes, partnerMinutes,
                row.noteOf(isA), row.noteOf(!isA), both, (int) row.totalMinutes(), hint);
    }

    // ========== 小件 ==========

    /** 本周周一；今天就是周一时返回今天。 */
    private LocalDate weekStart(LocalDate day) {
        return day.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** F365 周连击：今天往前数连续「双点」天数（今天没双点则看昨天起算，读到断为止）。 */
    private int streak(CoupleSpace space, LocalDate now) {
        List<CoupleFocusUnplug> rows = unplugMapper.findByYear(space.getId(), String.valueOf(now.getYear()));
        int count = 0;
        LocalDate cursor = now;
        while (count < 400) {
            String day = cursor.toString();
            boolean both = rows.stream()
                    .anyMatch(u -> day.equals(u.getDay()) && u.bothTicked());
            if (both) {
                count++;
                cursor = cursor.minusDays(1);
                continue;
            }
            if (cursor.equals(now)) {
                cursor = cursor.minusDays(1);
                continue;
            }
            break;
        }
        return count;
    }

    private boolean inRange(String day, String fromDay, String toDay) {
        return day != null && day.compareTo(fromDay) >= 0 && day.compareTo(toDay) <= 0;
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

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
