package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异地恋·时空同步（F110-F119）核心逻辑单测：牵手懒签到与双向推送、想念双向奔赴、
 * 作息重叠计算、见面信拆封条件、云约会兜底灵感、平安卡类型校验、见面日记补记与间隔、
 * 能量瓶充能曲线、异地恋报告聚合。
 */
@ExtendWith(MockitoExtension.class)
class CoupleDistanceServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleHandholdMapper handholdMapper;
    @Mock
    private CoupleMissDailyMapper missMapper;
    @Mock
    private CoupleRoutineMapper routineMapper;
    @Mock
    private CoupleReunionLetterMapper letterMapper;
    @Mock
    private CoupleCloudDateMapper cloudDateMapper;
    @Mock
    private CoupleSafetyPingMapper safetyMapper;
    @Mock
    private CoupleReunionLogMapper reunionMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleDistanceService distanceService;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis() - 100L * 24 * 3600 * 1000);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    // ========== F110 隔空牵手 ==========

    @Test
    void firstHoldCreatesRowWithoutBothPush() {
        stubSpace("alice");
        CoupleHandhold after = CoupleHandhold.of("s1", LocalDate.now().toString());
        after.setHoldA(1);
        after.setHoldAtA(System.currentTimeMillis());
        when(handholdMapper.findByDay("s1", LocalDate.now().toString())).thenReturn(null, after);
        when(handholdMapper.countBoth("s1")).thenReturn(0L);
        when(handholdMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleDistanceService.HandholdVO vo = distanceService.holdHand("alice");

        assertThat(vo.todayMine()).isTrue();
        assertThat(vo.todayBoth()).isFalse();
        verify(handholdMapper).insert(any(CoupleHandhold.class));
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void bothHoldTriggersBothPushWithMilestone() {
        stubSpace("alice");
        CoupleHandhold row = CoupleHandhold.of("s1", LocalDate.now().toString());
        row.setHoldB(1);
        row.setHoldAtB(System.currentTimeMillis());
        when(handholdMapper.findByDay("s1", LocalDate.now().toString())).thenReturn(row);
        when(handholdMapper.countBoth("s1")).thenReturn(5L);
        when(handholdMapper.findBySpace("s1")).thenReturn(List.of(row));

        CoupleDistanceService.HandholdVO vo = distanceService.holdHand("alice");

        assertThat(vo.todayBoth()).isTrue();
        assertThat(vo.totalDays()).isEqualTo(5);
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), detail.capture());
        assertThat(detail.getValue()).contains("5");
    }

    // ========== F112 想念计量所 ==========

    @Test
    void bothMissMarksBothAtAndPushes() {
        stubSpace("alice");
        CoupleMissDaily row = CoupleMissDaily.of("s1", LocalDate.now().toString());
        row.setMissB(1);
        row.setMissAtB(System.currentTimeMillis());
        when(missMapper.findByDay("s1", LocalDate.now().toString())).thenReturn(row);
        when(missMapper.countBoth("s1")).thenReturn(3L);
        when(missMapper.findBySpace("s1")).thenReturn(List.of(row));

        CoupleDistanceService.MissVO vo = distanceService.lightMiss("alice");

        assertThat(vo.todayBoth()).isTrue();
        assertThat(row.getBothAt()).isNotNull();
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F114 作息表与重叠 ==========

    @Test
    void routineOverlapComputed() {
        stubSpace("alice");
        CoupleRoutine mine = CoupleRoutine.of("s1", "alice", "07:00", "09:00", "18:00", "23:00");
        CoupleRoutine partner = CoupleRoutine.of("s1", "bob", "08:00", "09:30", "17:30", "22:30");
        when(routineMapper.findBySpace("s1")).thenReturn(List.of(mine, partner));

        CoupleDistanceService.RoutineVO vo = distanceService.routine("alice");

        assertThat(vo.mine()).isNotNull();
        assertThat(vo.partner()).isNotNull();
        assertThat(vo.overlaps()).isNotEmpty();
        // 起床段交集：alice 07:00-09:00 × bob 08:00-09:30 = 08:00-09:00（60 分钟 ≥ 30）
        assertThat(vo.overlaps().get(0).start()).isEqualTo("08:00");
        assertThat(vo.overlaps().get(0).end()).isEqualTo("09:00");
    }

    @Test
    void saveRoutineValidatesTimeFormat() {
        stubSpace("alice");
        assertThatThrownBy(() -> distanceService.saveRoutine("alice", "7点", "09:00", "18:00", "23:00"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起床时间");
    }

    // ========== F115 见面信 ==========

    @Test
    void openLetterBeforeMeetupRejected() {
        stubSpace("alice");
        CoupleReunionLetter letter = CoupleReunionLetter.of("s1", "bob", "见面给你个抱抱");
        when(letterMapper.selectById(letter.getId())).thenReturn(letter);
        when(reunionMapper.findBySpace("s1")).thenReturn(List.of());
        assertThatThrownBy(() -> distanceService.openLetter("alice", letter.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没见面");
    }

    @Test
    void openLetterAfterLoggedMeetupSucceeds() {
        stubSpace("alice");
        CoupleReunionLog log = CoupleReunionLog.of("s1", "alice",
                LocalDate.now().toString(), "一起吃了火锅");
        CoupleReunionLetter letter = CoupleReunionLetter.of("s1", "bob", "见面给你个抱抱");
        when(letterMapper.selectById(letter.getId())).thenReturn(letter);
        when(reunionMapper.findBySpace("s1")).thenReturn(List.of(log));
        when(letterMapper.findBySpace("s1")).thenReturn(List.of(letter));

        List<CoupleDistanceService.LetterVO> letters = distanceService.openLetter("alice", letter.getId());

        assertThat(letter.getStatus()).isEqualTo(CoupleReunionLetter.STATUS_OPENED);
        assertThat(letters.get(0).content()).isEqualTo("见面给你个抱抱");
    }

    @Test
    void letterContentHiddenUntilOpenedForPartner() {
        stubSpace("alice");
        CoupleReunionLetter letter = CoupleReunionLetter.of("s1", "bob", "小秘密");
        when(letterMapper.findBySpace("s1")).thenReturn(List.of(letter));
        when(reunionMapper.findBySpace("s1")).thenReturn(List.of());

        List<CoupleDistanceService.LetterVO> letters = distanceService.reunionLetters("alice");

        assertThat(letters.get(0).content()).isNull();
        assertThat(letters.get(0).mine()).isFalse();
    }

    // ========== F116 云约会 ==========

    @Test
    void addCloudDateFallsBackToSuggestion() {
        stubSpace("alice");
        when(cloudDateMapper.findBySpace("s1")).thenReturn(List.of());
        when(cloudDateMapper.findBySpace("s1")).thenReturn(List.of());
        distanceService.addCloudDate("alice", null);
        ArgumentCaptor<CoupleCloudDate> captor = ArgumentCaptor.forClass(CoupleCloudDate.class);
        verify(cloudDateMapper).insert(captor.capture());
        assertThat(captor.getValue().getItem()).isNotBlank();
    }

    @Test
    void doneCloudDateRequiresExistingRow() {
        stubSpace("alice");
        when(cloudDateMapper.selectById("nope")).thenReturn(null);
        assertThatThrownBy(() -> distanceService.doneCloudDate("alice", "nope", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有找到");
    }

    // ========== F117 平安卡 ==========

    @Test
    void safetyPingValidatesKind() {
        stubSpace("alice");
        assertThatThrownBy(() -> distanceService.pingSafety("alice", "FLY", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("平安卡");
    }

    @Test
    void arrivePingPushesPartner() {
        stubSpace("alice");
        when(safetyMapper.findBySpace("s1")).thenReturn(List.of());
        distanceService.pingSafety("alice", CoupleSafetyPing.KIND_ARRIVE, "电梯坏了爬的楼");
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F118 见面日记 ==========

    @Test
    void reunionCannotBeInFuture() {
        stubSpace("alice");
        String tomorrow = LocalDate.now().plusDays(1).toString();
        assertThatThrownBy(() -> distanceService.logReunion("alice", tomorrow, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经发生");
    }

    @Test
    void reunionDuplicateDayRejected() {
        stubSpace("alice");
        String day = LocalDate.now().toString();
        lenient().when(reunionMapper.findBySpace("s1"))
                .thenReturn(List.of(CoupleReunionLog.of("s1", "alice", day, null)));
        assertThatThrownBy(() -> distanceService.logReunion("alice", day, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经记过");
    }

    @Test
    void reunionIntervalsComputed() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        String before = LocalDate.now().minusDays(9).toString();
        lenient().when(reunionMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleReunionLog.of("s1", "alice", today, null),
                CoupleReunionLog.of("s1", "bob", before, null)));

        List<CoupleDistanceService.ReunionLogVO> logs = distanceService.reunions("alice");

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).intervalDays()).isEqualTo(9);
        assertThat(logs.get(1).intervalDays()).isNull();
    }

    // ========== F113 见面能量瓶 ==========

    @Test
    void energyFullWhenNeverMet() {
        stubSpace("alice");
        when(reunionMapper.findLatest("s1")).thenReturn(null);
        CoupleDistanceService.EnergyVO vo = distanceService.energy("alice");
        assertThat(vo.daysSince()).isNull();
        assertThat(vo.energy()).isEqualTo(100);
    }

    @Test
    void energyChargesByDaysSinceLastMeet() {
        stubSpace("alice");
        String fifteenDaysAgo = LocalDate.now().minusDays(15).toString();
        when(reunionMapper.findLatest("s1"))
                .thenReturn(CoupleReunionLog.of("s1", "alice", fifteenDaysAgo, null));
        CoupleDistanceService.EnergyVO vo = distanceService.energy("alice");
        assertThat(vo.daysSince()).isEqualTo(15);
        assertThat(vo.energy()).isEqualTo(50);
    }

    // ========== F119 异地恋报告 ==========

    @Test
    void reportAggregatesCounts() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        String tenDaysAgo = LocalDate.now().minusDays(10).toString();
        lenient().when(reunionMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleReunionLog.of("s1", "alice", today, null),
                CoupleReunionLog.of("s1", "bob", tenDaysAgo, null)));
        when(missMapper.countBoth("s1")).thenReturn(7L);
        when(handholdMapper.countBoth("s1")).thenReturn(9L);
        when(letterMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleReunionLetter.of("s1", "alice", "等你拆")));
        when(cloudDateMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleCloudDate.of("s1", "bob", "连麦看电影")));

        CoupleDistanceService.DistanceReportVO vo = distanceService.report("alice");

        assertThat(vo.meetCount()).isEqualTo(2);
        assertThat(vo.avgIntervalDays()).isEqualTo(10);
        assertThat(vo.missBothDays()).isEqualTo(7);
        assertThat(vo.handholdDays()).isEqualTo(9);
        assertThat(vo.sealedLetters()).isEqualTo(1);
        assertThat(vo.summary()).contains("双向奔赴 7 次");
    }
}
