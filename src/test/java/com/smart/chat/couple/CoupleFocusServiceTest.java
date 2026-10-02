package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注意力保护区（F360-F369）单测：专注打卡分钟数钳制（上/下界）与超长一句话 400、单报与双报推不同事件；
 * 专属时段 day 越出本周 400、hours 钳制、自确认 400、无人预约 400、对方确认幂等不重复推；
 * 攒一句话 80 字上限与在途 5 条上限、收全部只回执本次真置读的条数、GET today 惰性签收不推事件；
 * 饭桌/对视/不插电双点各推一次且重复点击不重复推（回归保护）、不插电周连击；
 * 走神温柔哨每人每天 2 张限流与额度递减、哨语 40 字上限；
 * 数字排毒 kind 只收 AM/PM、双报达成且重复应战不重复推；
 * 专注周报与注意力年报的真数字（小时数/最专注一天）、年份格式 400；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleFocusServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleFocusNightMapper nightMapper;
    @Mock
    private CoupleFocusSlotMapper slotMapper;
    @Mock
    private CoupleFocusQueueMapper queueMapper;
    @Mock
    private CoupleFocusMealMapper mealMapper;
    @Mock
    private CoupleFocusGazeMapper gazeMapper;
    @Mock
    private CoupleFocusUnplugMapper unplugMapper;
    @Mock
    private CoupleFocusNudgeMapper nudgeMapper;
    @Mock
    private CoupleFocusDetoxMapper detoxMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleFocusService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());
    private static final String WEEK = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();

    private final List<CoupleFocusNight> nights = new ArrayList<>();
    private final List<CoupleFocusSlot> slots = new ArrayList<>();
    private final List<CoupleFocusQueue> queues = new ArrayList<>();
    private final List<CoupleFocusMeal> meals = new ArrayList<>();
    private final List<CoupleFocusGaze> gazes = new ArrayList<>();
    private final List<CoupleFocusUnplug> unplugs = new ArrayList<>();
    private final List<CoupleFocusNudge> nudges = new ArrayList<>();
    private final List<CoupleFocusDetox> detoxes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setAnniversary(DAY);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(nightMapper.findByDay("s1", DAY)).thenAnswer(inv -> nights.stream()
                .filter(n -> DAY.equals(n.getDay())).findFirst().orElse(null));
        lenient().when(nightMapper.findByDayRange(eq("s1"), any(), any())).thenAnswer(inv -> nights.stream()
                .filter(n -> inRange(n.getDay(), inv.getArgument(1), inv.getArgument(2))).toList());
        lenient().when(nightMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> nights.stream()
                .filter(n -> n.getDay().startsWith((String) inv.getArgument(1))).toList());
        stubUpsert(nightMapper, CoupleFocusNight.class, nights);

        lenient().when(slotMapper.findByWeek("s1", WEEK)).thenAnswer(inv -> slots.stream()
                .filter(s -> WEEK.equals(s.getWeek())).findFirst().orElse(null));
        lenient().when(slotMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(slots));
        stubUpsert(slotMapper, CoupleFocusSlot.class, slots);

        lenient().when(queueMapper.findByToUser("s1", "bob")).thenAnswer(inv -> queues.stream()
                .filter(q -> "bob".equals(q.getToUser())).toList());
        lenient().when(queueMapper.findByToUser("s1", "alice")).thenAnswer(inv -> queues.stream()
                .filter(q -> "alice".equals(q.getToUser())).toList());
        lenient().when(queueMapper.findByFromUser(eq("s1"), any())).thenAnswer(inv -> queues.stream()
                .filter(q -> q.getFromUser().equals(inv.getArgument(1))).toList());
        stubInsert(queueMapper, CoupleFocusQueue.class, queues);

        lenient().when(mealMapper.findByDay("s1", DAY)).thenAnswer(inv -> meals.stream()
                .filter(m -> DAY.equals(m.getDay())).findFirst().orElse(null));
        lenient().when(mealMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(meals));
        stubUpsert(mealMapper, CoupleFocusMeal.class, meals);

        lenient().when(gazeMapper.findByDay("s1", DAY)).thenAnswer(inv -> gazes.stream()
                .filter(g -> DAY.equals(g.getDay())).findFirst().orElse(null));
        lenient().when(gazeMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> gazes.stream()
                .filter(g -> g.getDay().startsWith((String) inv.getArgument(1))).toList());
        stubUpsert(gazeMapper, CoupleFocusGaze.class, gazes);

        lenient().when(unplugMapper.findByDay("s1", DAY)).thenAnswer(inv -> unplugs.stream()
                .filter(u -> DAY.equals(u.getDay())).findFirst().orElse(null));
        lenient().when(unplugMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> unplugs.stream()
                .filter(u -> u.getDay().startsWith((String) inv.getArgument(1))).toList());
        stubUpsert(unplugMapper, CoupleFocusUnplug.class, unplugs);

        lenient().when(nudgeMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> nudges.stream()
                .filter(n -> n.getDay().equals(inv.getArgument(1)) && n.getFromUser().equals(inv.getArgument(2)))
                .toList());
        lenient().when(nudgeMapper.findByDay("s1", DAY)).thenAnswer(inv -> List.copyOf(nudges));
        lenient().when(nudgeMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> nudges.stream()
                .filter(n -> n.getDay().startsWith((String) inv.getArgument(1))).toList());
        stubInsert(nudgeMapper, CoupleFocusNudge.class, nudges);

        lenient().when(detoxMapper.findByDay("s1", DAY)).thenAnswer(inv -> detoxes.stream()
                .filter(d -> DAY.equals(d.getDay())).findFirst().orElse(null));
        lenient().when(detoxMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(detoxes));
        stubUpsert(detoxMapper, CoupleFocusDetox.class, detoxes);
    }

    /** insert/updateById 通用桩：实体 of() 工厂已写 created，insert 只需入袋；updateById 就地生效（袋里是同一引用）。 */
    private <T> void stubUpsert(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type, List<T> bag) {
        lenient().when(mapper.insert(any(type))).thenAnswer(inv -> {
            bag.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(mapper.updateById(any(type))).thenReturn(1);
    }

    private <T> void stubInsert(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type, List<T> bag) {
        lenient().when(mapper.insert(any(type))).thenAnswer(inv -> {
            bag.add(inv.getArgument(0));
            return 1;
        });
    }

    private boolean inRange(String day, Object fromDay, Object toDay) {
        return day != null && day.compareTo((String) fromDay) >= 0 && day.compareTo((String) toDay) <= 0;
    }

    private CoupleFocusNight seedNight(String day, Integer minutesA, Integer minutesB) {
        CoupleFocusNight row = CoupleFocusNight.of("s1", day);
        row.setMinutesA(minutesA);
        row.setMinutesB(minutesB);
        nights.add(row);
        return row;
    }

    // ========== F360 专注打卡 ==========

    @Test
    void nightClampsUpperBoundAndPushesOneSide() {
        CoupleFocusService.TodayVO vo = service.night("alice", 999, "把手机锁抽屉里了");

        assertThat(vo.night().mineMinutes()).isEqualTo(CoupleFocusNight.MINUTES_MAX);
        assertThat(vo.night().mineReported()).isTrue();
        assertThat(vo.night().bothLit()).isFalse();
        verify(push).pushCoupleEventBoth(eq("focus-night-reported"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    @Test
    void nightClampsNegativeToZero() {
        assertThat(service.night("alice", -50, null).night().mineMinutes())
                .isEqualTo(CoupleFocusNight.MINUTES_MIN);
    }

    @Test
    void nightBothReportedLightsUpAndPushesLit() {
        service.night("alice", 60, "A");

        CoupleFocusService.TodayVO vo = service.night("bob", 30, "B");

        assertThat(vo.night().bothLit()).isTrue();
        assertThat(vo.night().totalMinutes()).isEqualTo(90);
        verify(push).pushCoupleEventBoth(eq("focus-night-lit"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    @Test
    void nightNoteOverLimitRejects() {
        assertThatThrownBy(() -> service.night("alice", 30, "字".repeat(CoupleFocusNight.NOTE_MAX + 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多");
    }

    // ========== F361 专属时段 ==========

    @Test
    void slotProposeOutsideCurrentWeekRejects() {
        String nextWeekDay = LocalDate.now().plusDays(9).toString();

        assertThatThrownBy(() -> service.proposeSlot("alice", "一起做饭", 2, nextWeekDay))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本周");
    }

    @Test
    void slotProposeClampsHoursAndRejectsBlankTitle() {
        CoupleFocusService.TodayVO vo = service.proposeSlot("alice", "  散步两圈  ", 99, DAY);

        assertThat(vo.slot().hours()).isEqualTo(CoupleFocusSlot.HOURS_MAX);
        assertThat(vo.slot().title()).isEqualTo("散步两圈");
        assertThat(vo.slot().confirmed()).isFalse();
        assertThatThrownBy(() -> service.proposeSlot("alice", "   ", 2, DAY))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void slotProposerCannotConfirmOwnSlot() {
        service.proposeSlot("alice", "看电影", 2, DAY);

        assertThatThrownBy(() -> service.confirmSlot("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己");
    }

    @Test
    void slotConfirmWithoutAnyProposalRejects() {
        assertThatThrownBy(() -> service.confirmSlot("bob"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没有人预约");
    }

    @Test
    void slotConfirmByPartnerIsIdempotent() {
        service.proposeSlot("alice", "看电影", 2, DAY);

        assertThat(service.confirmSlot("bob").slot().confirmed()).isTrue();
        service.confirmSlot("bob");

        verify(push, times(1)).pushCoupleEventBoth(eq("focus-slot-confirmed"), any(), any(), any(), any());
    }

    // ========== F362 攒一句话 ==========

    @Test
    void queueContentOverLimitRejects() {
        assertThatThrownBy(() -> service.queueAdd("alice", "字".repeat(CoupleFocusQueue.CONTENT_MAX + 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多");
    }

    @Test
    void queueInFlightLimitPerUser() {
        for (int i = 0; i < CoupleFocusQueue.IN_FLIGHT_MAX; i++) {
            service.queueAdd("alice", "第 " + i + " 句");
        }

        assertThatThrownBy(() -> service.queueAdd("alice", "超出的那句"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等 TA 收一下");
        assertThat(service.today("alice").queueUnread()).isZero();
    }

    @Test
    void queueReadPushesOnlyRealNewlyRead() {
        service.queueAdd("alice", "第一句");
        service.queueAdd("alice", "第二句");

        assertThat(service.queueRead("bob").queue()).hasSize(2);
        service.queueRead("bob");

        verify(push, times(1)).pushCoupleEventBoth(eq("focus-queue-read"), any(), any(), any(), any());
    }

    @Test
    void readingTodayMustNotAutoSettleOtherwiseSignOffDies() {
        service.queueAdd("alice", "偷偷攒的一句");

        // 打开总览就把留言置已读 → queueUnread 恒 0、/queue/read 恒签 0 条、focus-queue-read 永不推，
        // 「一键收全部」这个功能会被页面加载吃掉，所以读接口只读不签
        CoupleFocusService.TodayVO vo = service.today("bob");
        assertThat(vo.queueUnread()).isEqualTo(1);
        verify(push, never()).pushCoupleEventBoth(eq("focus-queue-read"), any(), any(), any(), any());

        CoupleFocusService.TodayVO signed = service.queueRead("bob");
        assertThat(signed.queueUnread()).isZero();
        verify(push).pushCoupleEventBoth(eq("focus-queue-read"), any(), any(), any(), any());
    }

    // ========== F363/F364/F365 双点打卡 ==========

    @Test
    void mealBothTicksPushOnceAndRepeatedClickStaysSilent() {
        service.mealTick("alice");
        assertThat(service.mealTick("bob").mealBoth()).isTrue();

        service.mealTick("bob");
        service.mealTick("alice");

        verify(push, times(1)).pushCoupleEventBoth(eq("focus-meal-both"), any(), any(), any(), any());
        assertThat(service.today("alice").meals()).isEqualTo(2);
    }

    @Test
    void doubleTickCardsReportWhoAlreadyTapped() {
        // TodayVO 原本只给「几人点了 + 是否双点」，不给「我这一趟按过没」，
        // 前端只能自己本地记，重进页面就把「我已经点了」误显示成「就差你一个」
        assertThat(service.today("alice").mealMine()).isFalse();
        service.mealTick("alice");
        assertThat(service.today("alice").mealMine()).isTrue();
        assertThat(service.today("bob").mealMine()).isFalse();
        assertThat(service.today("bob").mealBoth()).isFalse();

        service.gazeTick("bob");
        assertThat(service.today("bob").gazeMine()).isTrue();
        assertThat(service.today("alice").gazeMine()).isFalse();

        service.detox("alice", "AM");
        assertThat(service.today("alice").detoxMine()).isTrue();
        assertThat(service.today("bob").detoxMine()).isFalse();
        assertThat(service.today("bob").detoxKind()).isEqualTo("AM");
    }

    @Test
    void gazeBothTicksLightsUp() {
        assertThat(service.gazeTick("alice").gazeBoth()).isFalse();

        CoupleFocusService.TodayVO vo = service.gazeTick("bob");

        assertThat(vo.gazeBoth()).isTrue();
        assertThat(vo.gazes()).isEqualTo(2);
        verify(push).pushCoupleEventBoth(eq("focus-gaze-both"), any(), any(), any(), any());

        service.gazeTick("bob");
        verify(push, times(1)).pushCoupleEventBoth(eq("focus-gaze-both"), any(), any(), any(), any());
    }

    @Test
    void unplugStreakCountsConsecutiveBothDays() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CoupleFocusUnplug prev = CoupleFocusUnplug.of("s1", yesterday.toString());
        prev.tick(true);
        prev.tick(false);
        unplugs.add(prev);

        assertThat(service.unplugTick("alice").unplugBoth()).isFalse();
        CoupleFocusService.TodayVO vo = service.unplugTick("bob");

        // streak 只读当年记录，元旦当天昨天的双点不在窗口内
        int expected = yesterday.getYear() == LocalDate.now().getYear() ? 2 : 1;
        assertThat(vo.unplugBoth()).isTrue();
        assertThat(vo.unplugStreak()).isEqualTo(expected);
        verify(push, times(1)).pushCoupleEventBoth(eq("focus-unplug-both"), any(), any(), any(), any());
    }

    // ========== F366 走神温柔哨 ==========

    @Test
    void nudgeDailyQuotaTwoPerUser() {
        CoupleFocusService.TodayVO afterFirst = service.nudge("alice", "");
        assertThat(afterFirst.nudgeQuotaLeft()).isEqualTo(CoupleFocusNudge.DAILY_MAX - 1);
        assertThat(afterFirst.nudgesToday()).isEqualTo(1);

        service.nudge("alice", null);
        assertThat(service.nudge("bob", "回来啦").nudgesToday()).isEqualTo(3);

        assertThatThrownBy(() -> service.nudge("alice", "第三张"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用完");
    }

    @Test
    void nudgeNoteOverLimitRejects() {
        assertThatThrownBy(() -> service.nudge("alice", "字".repeat(CoupleFocusNudge.NOTE_MAX + 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多");
    }

    // ========== F368 数字排毒半天 ==========

    @Test
    void detoxRejectsUnknownKind() {
        assertThatThrownBy(() -> service.detox("alice", "NOON"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AM 或 PM");
    }

    @Test
    void detoxStartsThenCompletesOnce() {
        service.detox("alice", "am");
        assertThat(service.detox("bob", "PM").detoxBoth()).isTrue();

        service.detox("bob", "PM");

        verify(push, times(1)).pushCoupleEvent(eq("focus-detox-started"), eq("alice"), eq("bob"), any());
        verify(push, times(1)).pushCoupleEventBoth(eq("focus-detox-done"), any(), any(), any(), any());
    }

    // ========== F367 专注周报 ==========

    @Test
    void weeklyAggregatesRealNumbers() {
        seedNight(WEEK, 40, 20);
        seedNight(LocalDate.now().minusDays(20).toString(), 50, 50);
        CoupleFocusMeal meal = CoupleFocusMeal.of("s1", DAY);
        meal.tick(true);
        meal.tick(false);
        meals.add(meal);
        CoupleFocusGaze gaze = CoupleFocusGaze.of("s1", DAY);
        gaze.tick(true);
        gaze.tick(false);
        gazes.add(gaze);
        service.proposeSlot("alice", "看电影", 2, DAY);
        service.confirmSlot("bob");
        service.nudge("alice", "回来啦");

        CoupleFocusService.WeeklyVO vo = service.weekly("alice");

        assertThat(vo.week()).isEqualTo(WEEK);
        assertThat(vo.minutes()).isEqualTo(60);
        assertThat(vo.litNights()).isEqualTo(1);
        assertThat(vo.meals()).isEqualTo(1);
        assertThat(vo.gazes()).isEqualTo(1);
        assertThat(vo.slots()).isEqualTo(1);
        assertThat(vo.nudges()).isEqualTo(1);
        assertThat(vo.summary()).isNotBlank();
    }

    @Test
    void weeklyRejectsWhenNoSpace() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.weekly("carol"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }

    // ========== F369 注意力年报 ==========

    @Test
    void yearReportHoursAndTopDay() {
        seedNight(LocalDate.now().minusDays(3).toString(), 30, 30);
        seedNight(DAY, 120, 60);
        CoupleFocusDetox detox = CoupleFocusDetox.of("s1", DAY, CoupleFocusDetox.KIND_AM);
        detox.tick(true, "alice");
        detox.tick(false, "bob");
        detoxes.add(detox);

        CoupleFocusService.YearlyVO vo = service.yearReport("alice", null);

        assertThat(vo.year()).isEqualTo(LocalDate.now().getYear());
        assertThat(vo.minutes()).isEqualTo(240);
        assertThat(vo.hours()).isEqualTo("4.0");
        assertThat(vo.litNights()).isEqualTo(2);
        assertThat(vo.detox()).isEqualTo(1);
        assertThat(vo.topDay()).isEqualTo(DAY);
        assertThat(vo.topMinutes()).isEqualTo(180);
    }

    @Test
    void yearReportRejectsMalformedYear() {
        assertThatThrownBy(() -> service.yearReport("alice", "202"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yyyy");
    }

    @Test
    void everyReadOrWriteWithoutSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.today("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.night("carol", 10, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.queueRead("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.yearReport("carol", YEAR)).isInstanceOf(BusinessException.class);
    }
}
