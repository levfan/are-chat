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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 懂我与被接住核心逻辑单测：求抱抱回应链路、矛盾复盘双份合成、道歉券规则、
 * 树洞匿名与揭晓、心灵感应结算、情话储蓄罐利息。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCareTalkServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @Mock
    private CoupleComfortMapper comfortMapper;

    @Mock
    private CoupleMoodMapper moodMapper;

    @Mock
    private CouplePeaceReviewMapper reviewMapper;

    @Mock
    private CoupleSorryTicketMapper sorryMapper;

    @Mock
    private CoupleTruthMapper truthMapper;

    @Mock
    private CoupleWhisperMapper whisperMapper;

    @Mock
    private CoupleTelepathyMapper telepathyMapper;

    @Mock
    private CoupleLoveBankMapper loveBankMapper;

    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleComfortService comfortService;

    @InjectMocks
    private CoupleMakeupService makeupService;

    @InjectMocks
    private CoupleTalkService talkService;

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

    // ========== F60 求抱抱 ==========

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

    // ========== F61 矛盾复盘 ==========

    @Test
    void secondReviewOfTheDayTriggersPeacePouchForBoth() {
        stubSpace("bob");
        CouplePeaceReview alicePart = CouplePeaceReview.of("s1", "alice", "我说话太冲", "先听 TA 说完");
        when(reviewMapper.findByDay("s1", LocalDate.now().toString())).thenReturn(List.of(alicePart));
        when(reviewMapper.findBySpace("s1")).thenReturn(List.of(alicePart));

        makeupService.saveReview("bob", "我翻旧账", "就事论事");

        verify(reviewMapper).insert(any(CouplePeaceReview.class));
        verify(push).pushCoupleEventBoth(eq("peace-review-done"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void firstReviewOnlyNotifiesPartnerToWriteTheirPart() {
        stubSpace("alice");
        when(reviewMapper.findByDay("s1", LocalDate.now().toString())).thenReturn(List.of());
        when(reviewMapper.findBySpace("s1")).thenReturn(List.of());

        makeupService.saveReview("alice", "我太急了", "下次先抱抱再聊");

        verify(reviewMapper).insert(any(CouplePeaceReview.class));
        verify(push).pushCoupleEvent(eq("peace-review-kept"), eq("alice"), eq("bob"), anyString());
        verify(push, never()).pushCoupleEventBoth(any(), any(), any(), any(), any());
    }

    // ========== F62 道歉券 ==========

    @Test
    void sendSorryCapsActiveTicketsPerUser() {
        stubSpace("alice");
        CoupleSorryTicket active = CoupleSorryTicket.of("s1", "alice", "第一张");
        when(sorryMapper.findActiveByUser("s1", "alice")).thenReturn(List.of(active, CoupleSorryTicket.of("s1", "alice", "第二张")));

        assertThatThrownBy(() -> makeupService.sendSorry("alice", "对不起"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("道歉券");
        verify(sorryMapper, never()).insert(any(CoupleSorryTicket.class));
    }

    @Test
    void useSorryOnlyByReceiverAndNotifiesSender() {
        stubSpace("bob");
        CoupleSorryTicket ticket = CoupleSorryTicket.of("s1", "alice", "刚才语气不好，对不起");
        when(sorryMapper.selectById(ticket.getId())).thenReturn(ticket);
        when(sorryMapper.findBySpace("s1")).thenReturn(List.of(ticket));

        makeupService.useSorry("bob", ticket.getId(), "没事啦");

        assertThat(ticket.getStatus()).isEqualTo(CoupleSorryTicket.STATUS_USED);
        assertThat(ticket.getUsedNote()).isEqualTo("没事啦");
        verify(push).pushCoupleEvent(eq("sorry-used"), eq("bob"), eq("alice"), anyString());
    }

    @Test
    void useSorryRejectsSelfRedemption() {
        stubSpace("alice");
        CoupleSorryTicket ticket = CoupleSorryTicket.of("s1", "alice", "对不起");
        when(sorryMapper.selectById(ticket.getId())).thenReturn(ticket);

        assertThatThrownBy(() -> makeupService.useSorry("alice", ticket.getId(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等 TA 收");
    }

    // ========== F66 真心话 ==========

    @Test
    void answerTruthStoresAnswerAndBothShareSameQuestionPerDay() {
        stubSpace("alice");
        List<CoupleTruth> dayRows = new java.util.ArrayList<>();
        when(truthMapper.findByDay(eq("s1"), any(), anyString())).thenReturn(dayRows);
        org.mockito.Mockito.doAnswer(inv -> {
            dayRows.add(inv.getArgument(0));
            return 1;
        }).when(truthMapper).insert(any(CoupleTruth.class));

        CoupleTalkService.TruthTodayVO vo = talkService.answerTruth("alice", "怕你哭");

        assertThat(vo.question()).isNotBlank();
        assertThat(vo.myAnswer()).isEqualTo("怕你哭");
        verify(truthMapper).insert(any(CoupleTruth.class));
        verify(push).pushCoupleEvent(eq("truth-answered"), eq("alice"), eq("bob"), anyString());

        // 同一天 bob 看到的题目一致
        String bobQuestion = CoupleTalkBank.pickTruth("s1", LocalDate.now().toString());
        assertThat(CoupleTalkBank.pickTruth("s1", LocalDate.now().toString())).isEqualTo(bobQuestion);
    }

    // ========== F67 匿名树洞 ==========

    @Test
    void askWhisperLimitsOnePendingPerUser() {
        stubSpace("alice");
        CoupleWhisper pending = CoupleWhisper.of("s1", "alice", "在途的问题", true);
        when(whisperMapper.findPendingByUser("s1", "alice")).thenReturn(pending);

        assertThatThrownBy(() -> talkService.askWhisper("alice", "第二个问题", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("树洞");
    }

    @Test
    void answerWhisperRejectsSelfAnswerAndRevealsOnPartnerAnswer() {
        stubSpace("alice");
        CoupleWhisper mine = CoupleWhisper.of("s1", "alice", "我的问题", true);
        when(whisperMapper.selectById(mine.getId())).thenReturn(mine);

        assertThatThrownBy(() -> talkService.answerWhisper("alice", mine.getId(), "自答"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己回答");

        stubSpace("bob");
        when(whisperMapper.findBySpace("s1")).thenReturn(List.of(mine));
        talkService.answerWhisper("bob", mine.getId(), "骄傲过，偷偷很多次");

        assertThat(mine.getAnswer()).contains("骄傲");
        verify(push).pushCoupleEvent(eq("whisper-answered"), eq("bob"), eq("alice"), anyString());
    }

    @Test
    void anonymousPendingWhisperHidesAskerFromPartner() {
        stubSpace("bob");
        CoupleWhisper anonymous = CoupleWhisper.of("s1", "alice", "不敢问的", true);
        when(whisperMapper.findBySpace("s1")).thenReturn(List.of(anonymous));

        List<CoupleTalkService.WhisperVO> list = talkService.whispers("bob");

        assertThat(list.get(0).askerLabel()).isEqualTo("匿名小可爱");
        // 实名或已回答时对对方可见
        CoupleWhisper named = CoupleWhisper.of("s1", "alice", "实名的问题", false);
        when(whisperMapper.findBySpace("s1")).thenReturn(List.of(named));
        assertThat(talkService.whispers("bob").get(0).askerLabel()).isEqualTo("alice");
    }

    // ========== F68 心灵感应 ==========

    @Test
    void startTelepathyCapsThreeRoundsPerDay() {
        stubSpace("alice");
        when(telepathyMapper.countByDay("s1", LocalDate.now().toString())).thenReturn(3L);

        assertThatThrownBy(() -> talkService.startTelepathy("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 轮");
    }

    @Test
    void telepathySettlesWhenBothAnsweredAndMatchesCaseInsensitively() {
        stubSpace("bob");
        CoupleTelepathy round = CoupleTelepathy.of("s1", "今晚的月亮更像什么？");
        round.setRound(1);
        round.setAnswerA("  银币 ");
        when(telepathyMapper.findBySpace("s1")).thenReturn(List.of(round));

        talkService.answerTelepathy("bob", "银币");

        assertThat(round.bothAnswered()).isTrue();
        assertThat(round.matched()).isTrue();
        verify(telepathyMapper).updateById(round);
        verify(push).pushCoupleEventBoth(eq("telepathy-matched"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void telepathyDiffNotifiesBothWhenAnswersDiffer() {
        stubSpace("bob");
        CoupleTelepathy round = CoupleTelepathy.of("s1", "我们的爱情是什么颜色？");
        round.setRound(1);
        round.setAnswerA("粉色");
        when(telepathyMapper.findBySpace("s1")).thenReturn(List.of(round));

        talkService.answerTelepathy("bob", "蓝色");

        assertThat(round.matched()).isFalse();
        verify(push).pushCoupleEventBoth(eq("telepathy-diff"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F69 情话储蓄罐 ==========

    @Test
    void depositLoveStoresSilentlyAndDeliverInterestPicksOnePerNight() {
        stubSpace("alice");
        when(spaceMapper.findAllActive()).thenReturn(List.of(space()));
        when(loveBankMapper.findByUser("s1", "alice")).thenReturn(List.of());

        talkService.depositLove("alice", "今天你笑起来真好看");

        ArgumentCaptor<CoupleLoveBank> captor = ArgumentCaptor.forClass(CoupleLoveBank.class);
        verify(loveBankMapper).insert(captor.capture());
        assertThat(captor.getValue().getContent()).contains("笑");
        // 存入只发预告，不发内容
        verify(push).pushCoupleEvent(eq("love-bank-deposit"), eq("alice"), eq("bob"), anyString());

        CoupleLoveBank stored = captor.getValue();
        when(loveBankMapper.findUndelivered("s1")).thenReturn(List.of(stored));

        talkService.deliverInterest();

        assertThat(stored.isDelivered()).isTrue();
        assertThat(stored.getDeliveredAt()).isNotNull();
        verify(push, times(1)).pushCoupleEvent(eq("love-bank-interest"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void deliverInterestSkipsSpaceWithoutStock() {
        CoupleSpace space = space();
        when(spaceMapper.findAllActive()).thenReturn(List.of(space));
        when(loveBankMapper.findUndelivered("s1")).thenReturn(List.of());

        talkService.deliverInterest();

        verify(push, never()).pushCoupleEvent(any(), any(), any(), any());
    }

    // ========== F64 情绪同步率 / F65 深夜陪伴 ==========

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
