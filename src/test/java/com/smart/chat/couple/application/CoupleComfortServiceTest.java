package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleComfort;
import com.smart.chat.couple.infrastructure.persistence.CoupleComfortMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleMood;
import com.smart.chat.couple.infrastructure.persistence.CoupleMoodMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
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
 * 断言锁的是「真的写了一行、真的推给了对的人」，不是返回值回显。
 */
@ExtendWith(MockitoExtension.class)
class CoupleComfortServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @Mock
    private CoupleComfortMapper comfortMapper;

    @Mock
    private CoupleMoodMapper moodMapper;

    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleComfortService comfortService;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    @Test
    void askComfortCreatesTodayRowAndPushesPartner() {
        stubSpace("alice");
        when(comfortMapper.find("s1", "alice", LocalDate.now().toString())).thenReturn(null);
        when(comfortMapper.findBySpace("s1")).thenReturn(List.of());

        comfortService.askForComfort("alice", "SAD");

        ArgumentCaptor<CoupleComfort> captor = ArgumentCaptor.forClass(CoupleComfort.class);
        verify(comfortMapper).insert(captor.capture());
        assertThat(captor.getValue().getFeeling()).isEqualTo("SAD");
        assertThat(captor.getValue().getDay()).isEqualTo(LocalDate.now().toString());
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
        when(comfortMapper.find("s1", "bob", LocalDate.now().toString())).thenReturn(null);

        assertThatThrownBy(() -> comfortService.handleComfort("alice", "抱抱"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("求抱抱");
        verify(push, never()).pushCoupleEvent(any(), any(), any(), any());
    }

    @Test
    void handleComfortMarksHandledAndNotifiesAsker() {
        stubSpace("alice");
        CoupleComfort pending = CoupleComfort.of("s1", "bob", "WRONGED");
        when(comfortMapper.find("s1", "bob", LocalDate.now().toString())).thenReturn(pending);

        CoupleComfortService.ComfortVO handled = comfortService.handleComfort("alice", "你没错，先站你这边");

        assertThat(handled.handled()).isTrue();
        assertThat(handled.handledNote()).isEqualTo("你没错，先站你这边");
        verify(push).pushCoupleEvent(eq("comfort-given"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void moodSyncCountsSharedDaysAndStreak() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        CoupleMood myToday = CoupleMood.of("s1", "alice", today, "HAPPY", null);
        CoupleMood partnerToday = CoupleMood.of("s1", "bob", today, "HAPPY", null);
        CoupleMood myYesterday = CoupleMood.of("s1", "alice", yesterday, "CALM", null);
        CoupleMood partnerOther = CoupleMood.of("s1", "bob", yesterday, "SAD", null);
        when(moodMapper.findBySpace("s1")).thenReturn(List.of(myToday, partnerToday, myYesterday, partnerOther));

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
        when(spaceMapper.findAllActive()).thenReturn(List.of(space));
        String today = LocalDate.now().toString();
        CoupleMood aliceSad = CoupleMood.of("s1", "alice", today, "SAD", null);
        CoupleMood bobFine = CoupleMood.of("s1", "bob", today, "HAPPY", null);
        when(moodMapper.find("s1", "alice", today)).thenReturn(Optional.of(aliceSad));
        when(moodMapper.find("s1", "bob", today)).thenReturn(Optional.of(bobFine));
        CoupleComfort handled = CoupleComfort.of("s1", "alice", "SAD");
        handled.setHandled(true);
        when(comfortMapper.find("s1", "alice", today)).thenReturn(handled);

        comfortService.remindNightCare();

        verify(push, never()).pushCoupleEvent(any(), any(), any(), any());

        // 未被接住时提醒对方
        CoupleComfort untouched = CoupleComfort.of("s1", "alice", "SAD");
        when(comfortMapper.find("s1", "alice", today)).thenReturn(untouched);
        comfortService.remindNightCare();
        verify(push).pushCoupleEvent(eq("night-care"), eq("system"), eq("bob"), anyString());
    }
}
