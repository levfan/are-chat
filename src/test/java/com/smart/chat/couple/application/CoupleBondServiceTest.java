package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.bond.BondAction;
import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodReaction;
import com.smart.chat.couple.domain.mood.MoodReactionRepository;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.sharedkernel.web.BusinessException;
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
 * 贴贴与心情回应单测：动作流水真的落了一行、推送文案与事件名逐字没变、里程碑只在踩线那一次响、
 * 回应的三道闸门（目录／未来／TA 那天记没记）与「重复提交视为修改」都在端口这一层看得见。
 */
@ExtendWith(MockitoExtension.class)
class CoupleBondServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private ActionRepository actionRepository;
    @Mock
    private MoodReactionRepository moodReactionRepository;
    @Mock
    private MoodRepository moodRepository;
    @Mock
    private CoupleStreakService streakService;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleBondService service;

    private final String today = LocalDate.now().toString();

    private CoupleSpace space() {
        return CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null,
                null, null, null);
    }

    private void stubSpace(String me) {
        lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(space()));
    }

    // ========== 亲密小动作 ==========

    @Test
    void sendActionAppendsRowPushesPartnerAndTriesCheckin() {
        stubSpace("alice");

        CoupleBondService.BondStatsVO vo = service.sendAction("alice", "HUG");

        ArgumentCaptor<BondAction> captor = ArgumentCaptor.forClass(BondAction.class);
        verify(actionRepository).append(captor.capture());
        assertThat(captor.getValue().spaceId()).isEqualTo("s1");
        assertThat(captor.getValue().username()).isEqualTo("alice");
        assertThat(captor.getValue().kind()).isEqualTo("HUG");
        verify(push).pushCoupleEvent("bond-action", "alice", "bob", "TA 给了你一个大大的拥抱 🤗 快抱回去！");
        verify(streakService).markTodayAfterAction(any(CoupleSpace.class), eq("alice"));
        assertThat(vo.kinds()).extracting(CoupleBondService.KindStat::kind)
                .containsExactly("MISS", "HUG", "KISS", "POKE", "PAT", "NUZZLE", "TICKLE");
    }

    @Test
    void sendActionRejectsUnknownKindBeforeLookingUpSpace() {
        assertThatThrownBy(() -> service.sendAction("alice", "BITE"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不认识这个动作哦，换一个试试～");
        verify(spaceRepository, never()).findActiveByMember(anyString());
        verify(actionRepository, never()).append(any());
    }

    @Test
    void sendActionCelebratesOnlyTheExactMilestone() {
        stubSpace("alice");
        lenient().when(actionRepository.countByKind("s1", "KISS")).thenReturn(520L);
        service.sendAction("alice", "KISS");

        verify(push).pushCoupleEventBoth("bond-milestone", "alice", "alice", "bob",
                "第 520 次「亲亲」达成 🎉💋 你们好甜！");
    }

    @Test
    void sendActionStaysQuietOnePastTheMilestone() {
        stubSpace("alice");
        lenient().when(actionRepository.countByKind("s1", "KISS")).thenReturn(521L);

        service.sendAction("alice", "KISS");

        verify(push, never()).pushCoupleEventBoth(any(), any(), any(), any(), any());
    }

    @Test
    void sendActionIgnoresMilestonesForPlainActions() {
        stubSpace("alice");

        service.sendAction("alice", "POKE");

        verify(push, never()).pushCoupleEventBoth(any(), any(), any(), any(), any());
    }

    @Test
    void recentActionsDefaultsTo50AndMapsEveryColumn() {
        stubSpace("alice");
        BondAction a1 = BondAction.restore("a1", "s1", "bob", "MISS", 200L);
        BondAction a2 = BondAction.restore("a2", "s1", "alice", "HUG", 100L);
        lenient().when(actionRepository.listBySpace("s1")).thenReturn(List.of(a1, a2));

        assertThat(service.recentActions("alice", null))
                .containsExactly(new CoupleBondService.ActionVO("a1", "bob", "MISS", 200L),
                        new CoupleBondService.ActionVO("a2", "alice", "HUG", 100L));
        assertThat(service.recentActions("alice", 1)).hasSize(1);
        assertThat(service.recentActions("alice", 0)).hasSize(2);
    }

    @Test
    void statsCountsTodayUntilTheDayChanges() {
        stubSpace("alice");
        lenient().when(actionRepository.countByKind("s1", "HUG")).thenReturn(7L);
        lenient().when(actionRepository.countSentBy("s1", "HUG", "alice")).thenReturn(4L);
        lenient().when(actionRepository.countSentBy("s1", "HUG", "bob")).thenReturn(3L);
        lenient().when(actionRepository.lastSentAt("s1", "HUG")).thenReturn(999L);
        lenient().when(actionRepository.listBySpace("s1")).thenReturn(List.of(
                BondAction.restore("t1", "s1", "alice", "HUG", System.currentTimeMillis()),
                BondAction.restore("t2", "s1", "bob", "POKE", System.currentTimeMillis()),
                BondAction.restore("t3", "s1", "alice", "KISS",
                        System.currentTimeMillis() - 36L * 60 * 60 * 1000)));

        CoupleBondService.BondStatsVO vo = service.stats("alice");

        CoupleBondService.KindStat hug = vo.kinds().stream()
                .filter(k -> k.kind().equals("HUG")).findFirst().orElseThrow();
        assertThat(hug.emoji()).isEqualTo("🤗");
        assertThat(hug.label()).isEqualTo("抱抱");
        assertThat(hug.total()).isEqualTo(7L);
        assertThat(hug.mine()).isEqualTo(4L);
        assertThat(hug.partner()).isEqualTo(3L);
        assertThat(hug.lastAt()).isEqualTo(999L);
        assertThat(vo.todayMine()).isEqualTo(1L);
        assertThat(vo.todayPartner()).isEqualTo(1L);
        assertThat(vo.todayCount()).isEqualTo(2L);
    }

    // ========== 心情回应 ==========

    @Test
    void reactMoodInsertsFirstReactionAndPushesPartner() {
        stubSpace("alice");
        when(moodRepository.findBySpaceAndUserOn("s1", "bob", today))
                .thenReturn(Optional.of(mood("bob", today, "SAD")));
        when(moodReactionRepository.findBySpaceAndUserOn("s1", today, "alice")).thenReturn(Optional.empty());

        service.reactMood("alice", null, "HUG");

        ArgumentCaptor<MoodReaction> captor = ArgumentCaptor.forClass(MoodReaction.class);
        verify(moodReactionRepository).save(captor.capture());
        assertThat(captor.getValue().reaction()).isEqualTo("HUG");
        assertThat(captor.getValue().moodDay()).isEqualTo(today);
        assertThat(captor.getValue().fromUser()).isEqualTo("alice");
        assertThat(captor.getValue().updatedAt()).as("第一次贴回应只盖创建时刻").isNull();
        verify(push).pushCoupleEvent("mood-reacted", "alice", "bob",
                "TA 回应了你 " + today + " 的心情：抱抱 🤗");
    }

    @Test
    void reactMoodRevisesTheSameDaysReaction() {
        stubSpace("alice");
        when(moodRepository.findBySpaceAndUserOn("s1", "bob", today))
                .thenReturn(Optional.of(mood("bob", today, "SAD")));
        MoodReaction existing = MoodReaction.restore("r1", "s1", today, "alice", "KISS", 111L, 111L);
        when(moodReactionRepository.findBySpaceAndUserOn("s1", today, "alice")).thenReturn(Optional.of(existing));

        service.reactMood("alice", today, "CHEER");

        ArgumentCaptor<MoodReaction> captor = ArgumentCaptor.forClass(MoodReaction.class);
        verify(moodReactionRepository).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("r1");
        assertThat(captor.getValue().reaction()).isEqualTo("CHEER");
        assertThat(captor.getValue().created()).isEqualTo(111L);
        assertThat(captor.getValue().updatedAt()).isNotEqualTo(111L);
    }

    @Test
    void reactMoodRejectsReactionOutsideTheDirectory() {
        assertThatThrownBy(() -> service.reactMood("alice", today, "SIDE_HUG"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("回应只能是抱抱/亲亲/加油/摸摸头哦");
        verify(moodReactionRepository, never()).save(any());
    }

    @Test
    void reactMoodRejectsFutureDay() {
        stubSpace("alice");

        assertThatThrownBy(() -> service.reactMood("alice", LocalDate.now().plusDays(1).toString(), "HUG"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能回应未来的心情哦");
        verify(moodReactionRepository, never()).save(any());
    }

    @Test
    void reactMoodNeedsThePartnerToHaveRecordedThatDay() {
        stubSpace("alice");
        when(moodRepository.findBySpaceAndUserOn("s1", "bob", today)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reactMood("alice", today, "HUG"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("TA 那天还没记录心情，先提醒 TA 记一笔吧 💗");
        verify(moodReactionRepository, never()).save(any());
    }

    @Test
    void reactMoodRejectsBadDayFormat() {
        stubSpace("alice");

        assertThatThrownBy(() -> service.reactMood("alice", "2026-13-45", "HUG"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("日期格式应为 yyyy-MM-dd");
    }

    @Test
    void moodReactionsShowsBothSidesOfThatDay() {
        stubSpace("alice");
        when(moodReactionRepository.findBySpaceAndUserOn("s1", today, "alice"))
                .thenReturn(Optional.of(MoodReaction.restore("r1", "s1", today, "alice", "HUG", 1L, null)));
        when(moodReactionRepository.findBySpaceAndUserOn("s1", today, "bob")).thenReturn(Optional.empty());

        assertThat(service.moodReactions("alice", null))
                .isEqualTo(new CoupleBondService.MoodReactionVO(today, "HUG", null));
    }

    // ========== 专属爱称 ==========

    @Test
    void setPetNameSavesSpaceAndTellsPartner() {
        stubSpace("alice");

        String nick = service.setPetName("alice", "小饼");

        assertThat(nick).isEqualTo("小饼");
        verify(spaceRepository).save(any(CoupleSpace.class));
        verify(push).pushCoupleEvent("pet-name-changed", "alice", "bob",
                "TA 给你起了新的专属爱称：「小饼」，快去空间看看 🏷️");
    }

    @Test
    void noSpaceThrows404() {
        when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stats("ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }

    private Mood mood(String username, String day, String key) {
        return Mood.restore("m-" + username, "s1", username, day, key, null, 1L, null);
    }
}
