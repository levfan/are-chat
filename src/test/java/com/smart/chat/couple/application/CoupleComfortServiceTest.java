package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.comfort.ComfortRepository;
import com.smart.chat.couple.domain.comfort.ComfortRequest;
import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 求抱抱（保留卡 `couple-comfort`）核心链路单测：发出/回应/情绪同步率/深夜陪伴兜底。
 * 断言锁的是「真的存了一行、真的推给了对的人」，不是返回值回显。
 * 假表建在端口这一层（insert/update 的分流见 ComfortRepositoryAdapterTest），PO 与 Mapper 不出现在用例里。
 */
@ExtendWith(MockitoExtension.class)
class CoupleComfortServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;

    @Mock
    private ComfortRepository comfortRepository;

    @Mock
    private MoodRepository moodRepository;

    @Mock
    private ImPushService push;

    @org.mockito.InjectMocks
    private CoupleComfortService comfortService;

    private final String today = LocalDate.now().toString();

    private CoupleSpace space() {
        return CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null, null, null, null);
    }

    private void stubSpace(String me) {
        lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(space()));
    }

    @Test
    void askComfortCreatesTodayRowAndPushesPartner() {
        stubSpace("alice");
        when(comfortRepository.findBySpaceAndUserOn("s1", "alice", today)).thenReturn(Optional.empty());
        when(comfortRepository.listBySpace("s1")).thenReturn(List.of());

        comfortService.askForComfort("alice", "SAD");

        ArgumentCaptor<ComfortRequest> captor = ArgumentCaptor.forClass(ComfortRequest.class);
        verify(comfortRepository).save(captor.capture());
        assertThat(captor.getValue().feeling()).isEqualTo("SAD");
        assertThat(captor.getValue().day()).isEqualTo(today);
        verify(push).pushCoupleEvent(eq("comfort-sent"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void askComfortRejectsUnknownFeeling() {
        stubSpace("alice");
        assertThatThrownBy(() -> comfortService.askForComfort("alice", "HUNGRY"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("感受");
    }

    @Test
    void handleComfortRequiresPendingFromPartner() {
        stubSpace("alice");
        when(comfortRepository.findBySpaceAndUserOn("s1", "bob", today)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comfortService.handleComfort("alice", "抱抱"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("求抱抱");
        verify(push, never()).pushCoupleEvent(any(), any(), any(), any());
    }

    @Test
    void handleComfortMarksHandledAndNotifiesAsker() {
        stubSpace("alice");
        ComfortRequest pending = ComfortRequest.askOn("s1", "bob", today, "WRONGED");
        when(comfortRepository.findBySpaceAndUserOn("s1", "bob", today)).thenReturn(Optional.of(pending));

        CoupleComfortService.ComfortVO handled = comfortService.handleComfort("alice", "你没错，先站你这边");

        assertThat(handled.handled()).isTrue();
        assertThat(handled.handledNote()).isEqualTo("你没错，先站你这边");
        verify(push).pushCoupleEvent(eq("comfort-given"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void moodSyncCountsSharedDaysAndStreak() {
        stubSpace("alice");
        String yesterday = LocalDate.now().minusDays(1).toString();
        List<Mood> moods = List.of(
                mood("a1", "alice", today, "HAPPY"),
                mood("b1", "bob", today, "HAPPY"),
                mood("a2", "alice", yesterday, "CALM"),
                mood("b2", "bob", yesterday, "SAD"));
        when(moodRepository.listBySpace("s1")).thenReturn(moods);

        CoupleComfortService.MoodSyncVO vo = comfortService.moodSync("alice");

        assertThat(vo.bothDays()).isEqualTo(2);
        assertThat(vo.syncedDays()).isEqualTo(1);
        assertThat(vo.syncRate()).isEqualTo(50);
        assertThat(vo.todaySync()).isTrue();
        assertThat(vo.streak()).isEqualTo(1);
    }

    @Test
    void nightCareSkipsHandledComfortButRemindsUntouchedSadness() {
        CoupleSpace space = space();
        when(spaceRepository.findAllActive()).thenReturn(List.of(space));
        when(moodRepository.findBySpaceAndUserOn("s1", "alice", today))
                .thenReturn(Optional.of(mood("a1", "alice", today, "SAD")));
        when(moodRepository.findBySpaceAndUserOn("s1", "bob", today))
                .thenReturn(Optional.of(mood("b1", "bob", today, "HAPPY")));
        ComfortRequest handled = ComfortRequest.restore("c1", "alice", "SAD", "抱抱过了", 5L, true, "s1", today, 1L);
        when(comfortRepository.findBySpaceAndUserOn("s1", "alice", today)).thenReturn(Optional.of(handled));

        comfortService.remindNightCare();

        verify(push, never()).pushCoupleEvent(any(), any(), any(), any());

        // 未被接住时提醒对方
        ComfortRequest untouched = ComfortRequest.askOn("s1", "alice", today, "SAD");
        when(comfortRepository.findBySpaceAndUserOn("s1", "alice", today)).thenReturn(Optional.of(untouched));
        comfortService.remindNightCare();
        verify(push).pushCoupleEvent(eq("night-care"), eq("system"), eq("bob"), anyString());
    }

    private Mood mood(String id, String username, String day, String moodKey) {
        return Mood.restore(id, "s1", username, day, moodKey, null, 1L, null);
    }
}
