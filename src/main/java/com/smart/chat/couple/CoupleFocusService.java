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
    private final CoupleFocusMealMapper mealMapper;
    private final CoupleFocusGazeMapper gazeMapper;
    private final CoupleFocusNudgeMapper nudgeMapper;
    private final ImPushService push;

    public CoupleFocusService(CoupleSpaceMapper spaceMapper, CoupleFocusMealMapper mealMapper, CoupleFocusGazeMapper gazeMapper, CoupleFocusNudgeMapper nudgeMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.mealMapper = mealMapper;
        this.gazeMapper = gazeMapper;
        this.nudgeMapper = nudgeMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** 今日注意力总览：写接口一律原样返回这份聚合，前端整体替换。 */
    public record TodayVO(String day,
                          int meals, boolean mealMine, boolean mealBoth,
                          int gazes, boolean gazeMine, boolean gazeBoth,
                          int nudgesToday, int nudgeQuotaLeft) {
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

    // ========== F361 专属时段 ==========

    // ========== F362 攒一句话 ==========

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

    // ========== F367 专注周报 ==========

    // ========== F369 注意力年报 ==========

    // ========== 聚合 ==========

    private TodayVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        boolean isA = space.getUserA().equals(me);
        CoupleFocusMeal meal = mealMapper.findByDay(space.getId(), day);
        CoupleFocusGaze gaze = gazeMapper.findByDay(space.getId(), day);
        int nudgesToday = nudgeMapper.findByDay(space.getId(), day).size();

        return new TodayVO(day,
                meal == null ? 0 : (meal.ticked(true) ? 1 : 0) + (meal.ticked(false) ? 1 : 0),
                meal != null && meal.ticked(isA),
                meal != null && meal.bothTicked(),
                gaze == null ? 0 : (gaze.ticked(true) ? 1 : 0) + (gaze.ticked(false) ? 1 : 0),
                gaze != null && gaze.ticked(isA),
                gaze != null && gaze.bothTicked(),
                nudgesToday,
                Math.max(0, CoupleFocusNudge.DAILY_MAX
                        - nudgeMapper.findByDayUser(space.getId(), day, me).size()));
    }

    // ========== 小件 ==========

    /** 本周周一；今天就是周一时返回今天。 */
    private LocalDate weekStart(LocalDate day) {
        return day.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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
