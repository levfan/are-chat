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
 * 成长系（F150-F159）核心逻辑单测：习惯打卡流转与达成、重复打卡拦截、感恩便签、
 * 情绪颗粒度词表校验、每周高光双提名触发、拖延催办冷却与权限、优点存折、年度关键词聚合。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCoachServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleHabitStreakMapper streakMapper;
    @Mock
    private CoupleThanksNoteMapper thanksMapper;
    @Mock
    private CoupleFeelLogMapper feelMapper;
    @Mock
    private CoupleWeeklyStarMapper starMapper;
    @Mock
    private CoupleReadMinuteMapper readMapper;
    @Mock
    private CoupleDelayTaskMapper delayMapper;
    @Mock
    private CouplePraiseBankMapper praiseMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCoachService coachService;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis());
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    // ========== F150 习惯搭子 ==========

    @Test
    void createHabitRejectsSecondOpenHabit() {
        stubSpace("alice");
        when(streakMapper.findOpenByUser("s1", "alice")).thenReturn(CoupleHabitStreak.of("s1", "alice", "读书", 21));

        assertThatThrownBy(() -> coachService.createHabit("alice", "运动", 21))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("进行中");
        verify(streakMapper, never()).insert(any(CoupleHabitStreak.class));
    }

    @Test
    void createHabitValidatesTargetRange() {
        stubSpace("alice");
        assertThatThrownBy(() -> coachService.createHabit("alice", "喝水", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标天数");
    }

    @Test
    void checkinHabitRejectsSameDayTwice() {
        stubSpace("alice");
        CoupleHabitStreak habit = CoupleHabitStreak.of("s1", "alice", "读书", 21);
        habit.setLastDoneDay(LocalDate.now().toString());
        when(streakMapper.selectById(habit.getId())).thenReturn(habit);

        assertThatThrownBy(() -> coachService.checkinHabit("alice", habit.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经打过卡");
    }

    @Test
    void checkinHabitReachesTargetAndCompletes() {
        stubSpace("alice");
        CoupleHabitStreak habit = CoupleHabitStreak.of("s1", "alice", "读书", 21);
        habit.setDoneDays(20);
        when(streakMapper.selectById(habit.getId())).thenReturn(habit);
        when(streakMapper.findBySpace("s1")).thenReturn(List.of(habit));

        coachService.checkinHabit("alice", habit.getId());

        assertThat(habit.getStatus()).isEqualTo(CoupleHabitStreak.STATUS_DONE);
        assertThat(habit.getDoneDays()).isEqualTo(21);
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(event.capture(), anyString(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("streak-done");
    }

    @Test
    void checkinHabitOnlyByOwner() {
        stubSpace("bob");
        CoupleHabitStreak habit = CoupleHabitStreak.of("s1", "alice", "读书", 21);
        when(streakMapper.selectById(habit.getId())).thenReturn(habit);

        assertThatThrownBy(() -> coachService.checkinHabit("bob", habit.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己打卡");
    }

    // ========== F151 感恩便签 ==========

    @Test
    void addThanksPushesToPartner() {
        stubSpace("alice");
        when(thanksMapper.findBySpace("s1")).thenReturn(List.of());

        coachService.addThanks("alice", "谢谢你早上帮我热牛奶");

        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(event.capture(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("thanks-note");
    }

    // ========== F152 情绪颗粒度 ==========

    @Test
    void saveFeelRejectsUnknownWord() {
        stubSpace("alice");
        assertThatThrownBy(() -> coachService.saveFeel("alice", "不知道", 3, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("词表");
    }

    @Test
    void saveFeelCreatesThenUpdatesSameDay() {
        stubSpace("alice");
        when(feelMapper.find("s1", "alice", LocalDate.now().toString())).thenReturn(null);

        coachService.saveFeel("alice", "雀跃", 4, "被夸了");
        verify(feelMapper).insert(any(CoupleFeelLog.class));

        CoupleFeelLog existing = CoupleFeelLog.of("s1", "alice", LocalDate.now().toString(), "雀跃", 4, null);
        when(feelMapper.find("s1", "alice", LocalDate.now().toString())).thenReturn(existing);
        coachService.saveFeel("alice", "满足", 5, null);
        verify(feelMapper).updateById(existing);
        assertThat(existing.getWord()).isEqualTo("满足");
    }

    // ========== F153 每周高光 ==========

    @Test
    void weekStarBothNominatedTriggersBothPush() {
        stubSpace("alice");
        String monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
        when(starMapper.find("s1", monday, "alice")).thenReturn(null);
        when(starMapper.find("s1", monday, "bob")).thenReturn(CoupleWeeklyStar.of("s1", monday, "bob", "TA 主动做了早饭"));

        coachService.saveWeekStar("alice", "TA 深夜陪我改简历");

        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(event.capture(), anyString(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("week-star-both");
    }

    // ========== F154 共读一分钟 ==========

    @Test
    void readMinutePassageStableWithinDay() {
        stubSpace("alice");
        assertThat(coachService.readMinute("alice").passage())
                .isEqualTo(coachService.readMinute("alice").passage());
    }

    // ========== F155 拖延互助所 ==========

    @Test
    void nagDelayHasOneHourCooldown() {
        stubSpace("bob");
        CoupleDelayTask task = CoupleDelayTask.of("s1", "alice", "去体检", null);
        task.setLastNagAt(System.currentTimeMillis());
        when(delayMapper.selectById(task.getId())).thenReturn(task);

        assertThatThrownBy(() -> coachService.nagDelay("bob", task.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("小时");
    }

    @Test
    void nagDelayCannotTargetSelf() {
        stubSpace("alice");
        CoupleDelayTask task = CoupleDelayTask.of("s1", "alice", "去体检", null);
        when(delayMapper.selectById(task.getId())).thenReturn(task);

        assertThatThrownBy(() -> coachService.nagDelay("alice", task.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己");
    }

    @Test
    void doneDelayOnlyByOwnerAndCelebratesBoth() {
        CoupleDelayTask task = CoupleDelayTask.of("s1", "alice", "去体检", null);

        stubSpace("bob");
        when(delayMapper.selectById(task.getId())).thenReturn(task);
        assertThatThrownBy(() -> coachService.doneDelay("bob", task.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("立事人");

        stubSpace("alice");
        when(delayMapper.selectById(task.getId())).thenReturn(task);
        when(delayMapper.findBySpace("s1")).thenReturn(List.of(task));
        coachService.doneDelay("alice", task.getId());

        assertThat(task.getStatus()).isEqualTo(CoupleDelayTask.STATUS_DONE);
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(event.capture(), anyString(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("delay-done");
    }

    // ========== F158 优点存折 ==========

    @Test
    void addPraiseBankStoresScene() {
        stubSpace("alice");
        when(praiseMapper.findBySpace("s1")).thenReturn(List.of());

        coachService.addPraiseBank("alice", "吵架时从不翻旧账", "吵架时读");

        ArgumentCaptor<CouplePraiseBank> saved = ArgumentCaptor.forClass(CouplePraiseBank.class);
        verify(praiseMapper).insert(saved.capture());
        assertThat(saved.getValue().getScene()).isEqualTo("吵架时读");
    }

    // ========== F159 年度关键词 ==========

    @Test
    void yearKeywordPicksGratitudeWhenThanksDominate() {
        stubSpace("alice");
        List<CoupleThanksNote> notes = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> CoupleThanksNote.of("s1", "alice", "谢谢 " + i))
                .toList();
        when(thanksMapper.findBySpace("s1")).thenReturn(notes);
        when(feelMapper.findBySpace("s1")).thenReturn(List.of());
        when(streakMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleCoachService.YearKeywordVO vo = coachService.yearKeyword("alice", LocalDate.now().getYear());

        assertThat(vo.keyword()).isEqualTo("感恩力");
        assertThat(vo.thanksCount()).isEqualTo(12);
        assertThat(vo.summary()).contains("12");
    }

    @Test
    void yearKeywordEmptyStartsWithBeginner() {
        stubSpace("alice");
        when(thanksMapper.findBySpace("s1")).thenReturn(List.of());
        when(feelMapper.findBySpace("s1")).thenReturn(List.of());
        when(streakMapper.findBySpace("s1")).thenReturn(List.of());

        assertThat(coachService.yearKeyword("alice", null).keyword()).isEqualTo("刚开始");
    }
}
