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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 共同养成核心逻辑单测：挑战懒生成与双完成、存折连续天数、百日之约规则、
 * 心愿接单归属、下次一定催办冷却、共读双终点、追剧完结、词典与星座配对。
 */
@ExtendWith(MockitoExtension.class)
class CoupleGrowthServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @Mock
    private CoupleChallengeMapper challengeMapper;

    @Mock
    private CouplePassbookMapper passbookMapper;

    @Mock
    private CoupleHundredMapper hundredMapper;

    @Mock
    private CoupleHundredCheckinMapper checkinMapper;

    @Mock
    private CoupleWishExchangeMapper wishMapper;

    @Mock
    private CoupleTravelWishMapper travelMapper;

    @Mock
    private CoupleNextTimeMapper nextTimeMapper;

    @Mock
    private CoupleReadPlanMapper readPlanMapper;

    @Mock
    private CoupleReadProgressMapper readProgressMapper;

    @Mock
    private CoupleWatchlistMapper watchlistMapper;

    @Mock
    private CoupleDictWordMapper dictMapper;

    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleGrowthService growthService;

    @InjectMocks
    private CoupleGrowWishService wishService;

    @InjectMocks
    private CoupleEntertainService entertainService;

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

    // ========== F70 双人挑战赛 ==========

    @Test
    void challengeLazilyCreatesTodayRowWithStableQuestion() {
        stubSpace("alice");
        when(challengeMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleGrowthService.ChallengeBoardVO board = growthService.challenge("alice");

        ArgumentCaptor<CoupleChallenge> captor = ArgumentCaptor.forClass(CoupleChallenge.class);
        verify(challengeMapper).insert(captor.capture());
        assertThat(captor.getValue().getDay()).isEqualTo(LocalDate.now().toString());
        assertThat(captor.getValue().getTaskText()).isNotBlank();
        assertThat(board.today().taskText()).isNotBlank();
        // 同空间同天抽到的题一致
        assertThat(CoupleGrowthBank.pickChallenge("s1", LocalDate.now().toString()))
                .isEqualTo(CoupleGrowthBank.pickChallenge("s1", LocalDate.now().toString()));
    }

    @Test
    void secondCheckCompletesChallengeAndWinsForBoth() {
        stubSpace("bob");
        CoupleChallenge row = CoupleChallenge.of("s1", LocalDate.now().toString(), "一起夸对方三次");
        row.setDoneA(true); // alice 已完成
        when(challengeMapper.find("s1", LocalDate.now().toString())).thenReturn(row);
        when(challengeMapper.findBySpace("s1")).thenReturn(List.of(row));

        growthService.checkChallenge("bob");

        assertThat(row.bothDone()).isTrue();
        verify(push).pushCoupleEventBoth(eq("challenge-done"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F71 恋爱存折 ==========

    @Test
    void depositPassbookUpsertsTodayAndCountsStreak() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        CouplePassbook yesterdayRow = CouplePassbook.of("s1", "alice", "陪 TA 散步");
        yesterdayRow.setDay(yesterday);
        List<CouplePassbook> rows = new java.util.ArrayList<>(List.of(yesterdayRow));
        when(passbookMapper.findBySpace("s1")).thenReturn(rows);
        when(passbookMapper.find("s1", "alice", today)).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        }).when(passbookMapper).insert(any(CouplePassbook.class));

        CoupleGrowthService.PassbookBoardVO board = growthService.depositPassbook("alice", "给 TA 做了早饭");

        assertThat(board.myStreak()).isEqualTo(2);
        assertThat(board.mineToday()).isNotNull();
        verify(passbookMapper).insert(any(CouplePassbook.class));
        verify(push).pushCoupleEvent(eq("passbook-deposit"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F72 百日之约 ==========

    @Test
    void createHundredRejectsWhenActivePactExists() {
        stubSpace("alice");
        CoupleHundred active = CoupleHundred.of("s1", "一起早睡", LocalDate.now().toString());
        when(hundredMapper.findBySpace("s1")).thenReturn(List.of(active));

        assertThatThrownBy(() -> growthService.createHundred("alice", "一起运动", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("百日之约");
    }

    @Test
    void checkinHundredPushesProgressButNotDoneBeforeHundredDays() {
        stubSpace("alice");
        CoupleHundred pact = CoupleHundred.of("s1", "一起早睡", LocalDate.now().toString());
        when(hundredMapper.selectById(pact.getId())).thenReturn(pact);
        when(checkinMapper.find(pact.getId(), "alice", LocalDate.now().toString())).thenReturn(null);
        when(checkinMapper.findByPact(pact.getId())).thenReturn(List.of(CoupleHundredCheckin.of(pact.getId(), "s1", "alice", LocalDate.now().toString(), null)));

        growthService.checkinHundred("alice", pact.getId(), "今天早睡了");

        assertThat(pact.getStatus()).isEqualTo(CoupleHundred.STATUS_ACTIVE);
        verify(push).pushCoupleEvent(eq("hundred-checkin"), eq("alice"), eq("bob"), anyString());
        verify(push, never()).pushCoupleEventBoth(eq("hundred-done"), any(), any(), any(), any());
    }

    // ========== F73 心愿互换 ==========

    @Test
    void wishAcceptRejectsSelfAcceptButPartnerCanTakeIt() {
        stubSpace("alice");
        CoupleWishExchange wish = CoupleWishExchange.of("s1", "alice", "想要一杯奶茶");
        when(wishMapper.selectById(wish.getId())).thenReturn(wish);

        assertThatThrownBy(() -> wishService.acceptWish("alice", wish.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己接单");

        stubSpace("bob");
        wishService.acceptWish("bob", wish.getId());
        assertThat(wish.getStatus()).isEqualTo(CoupleWishExchange.STATUS_ACCEPTED);
        verify(push).pushCoupleEvent(eq("wish-accepted"), eq("bob"), eq("alice"), anyString());
    }

    // ========== F75 旅行心愿地图 ==========

    @Test
    void visitTravelMarksVisitedAndCelebratesBoth() {
        stubSpace("bob");
        CoupleTravelWish wish = CoupleTravelWish.of("s1", "alice", "青岛", "一起看海");
        when(travelMapper.selectById(wish.getId())).thenReturn(wish);
        when(travelMapper.findBySpace("s1")).thenReturn(List.of(wish));

        wishService.visitTravel("bob", wish.getId(), "海很蓝");

        assertThat(wish.isVisited()).isTrue();
        assertThat(wish.getVisitedNote()).isEqualTo("海很蓝");
        verify(push).pushCoupleEventBoth(eq("travel-visited"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F79 下次一定 ==========

    @Test
    void nudgeHasCooldownAndRejectsSelfNudge() {
        stubSpace("bob");
        stubSpace("alice");
        CoupleNextTime row = CoupleNextTime.of("s1", "alice", "下次陪你看日出");
        when(nextTimeMapper.selectById(row.getId())).thenReturn(row);

        assertThatThrownBy(() -> wishService.nudgeNextTime("alice", row.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己催自己");

        row.setNudgedAt(System.currentTimeMillis());
        assertThatThrownBy(() -> wishService.nudgeNextTime("bob", row.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 小时");
    }

    @Test
    void fulfillNextTimeOnlyByPromisor() {
        stubSpace("bob");
        CoupleNextTime row = CoupleNextTime.of("s1", "alice", "下次陪你看日出");
        when(nextTimeMapper.selectById(row.getId())).thenReturn(row);

        assertThatThrownBy(() -> wishService.fulfillNextTime("bob", row.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 自己兑现");

        stubSpace("alice");
        when(nextTimeMapper.findBySpace("s1")).thenReturn(List.of(row));
        wishService.fulfillNextTime("alice", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleNextTime.STATUS_DONE);
        verify(push).pushCoupleEvent(eq("nexttime-done"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F74 共读计划 ==========

    @Test
    void readProgressFinishesOnlyWhenBothReachTheEnd() {
        stubSpace("alice");
        stubSpace("bob");
        CoupleReadPlan plan = CoupleReadPlan.of("s1", "小王子", 3, "章");
        when(readPlanMapper.selectById(plan.getId())).thenReturn(plan);
        // alice 交第 3 章（终点），bob 只交到第 1 章
        CoupleReadProgress bobProgress = CoupleReadProgress.of(plan.getId(), "bob", 1, null);
        when(readProgressMapper.findByPlan(plan.getId())).thenReturn(List.of(bobProgress));

        entertainService.reportReadProgress("alice", plan.getId(), 3, "狐狸那段哭了");

        assertThat(plan.getStatus()).isEqualTo(CoupleReadPlan.STATUS_READING);
        verify(push).pushCoupleEvent(eq("read-progress"), eq("alice"), eq("bob"), anyString());

        // bob 也到终点 → 完结
        CoupleReadProgress aliceEnd = CoupleReadProgress.of(plan.getId(), "alice", 3, null);
        when(readProgressMapper.findByPlan(plan.getId())).thenReturn(List.of(aliceEnd, bobProgress));
        entertainService.reportReadProgress("bob", plan.getId(), 3, null);
        assertThat(plan.getStatus()).isEqualTo(CoupleReadPlan.STATUS_FINISHED);
        verify(push).pushCoupleEventBoth(eq("read-finished"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void readProgressRejectsOutOfRange() {
        stubSpace("alice");
        CoupleReadPlan plan = CoupleReadPlan.of("s1", "小王子", 3, "章");
        when(readPlanMapper.selectById(plan.getId())).thenReturn(plan);

        assertThatThrownBy(() -> entertainService.reportReadProgress("alice", plan.getId(), 9, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 ~ 3");
    }

    // ========== F76 追剧清单 ==========

    @Test
    void watchProgressAutoFinishesAtTotalUnits() {
        stubSpace("bob");
        CoupleWatchlist row = CoupleWatchlist.of("s1", "漫长的季节", 3);
        row.setCurrentUnit(2);
        when(watchlistMapper.selectById(row.getId())).thenReturn(row);

        entertainService.updateWatch("bob", row.getId(), 3);

        assertThat(row.getStatus()).isEqualTo(CoupleWatchlist.STATUS_DONE);
        assertThat(row.getUpdatedBy()).isEqualTo("bob");
        verify(push).pushCoupleEventBoth(eq("watch-finished"), eq("bob"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F78 恋爱词典 ==========

    @Test
    void addWordValidatesAndRemoveWordScopesToSpace() {
        stubSpace("alice");
        stubSpace("bob");
        assertThatThrownBy(() -> entertainService.addWord("alice", "", "释义"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("词汇");

        CoupleDictWord word = CoupleDictWord.of("s1", "alice", "小蛋糕", "生气只有三分钟");
        when(dictMapper.selectById(word.getId())).thenReturn(word);
        when(dictMapper.findBySpace("s1")).thenReturn(List.of());
        entertainService.removeWord("bob", word.getId());
        verify(dictMapper).deleteById(word.getId());

        CoupleDictWord foreign = CoupleDictWord.of("other", "mallory", "外星词", "不属于本空间");
        when(dictMapper.selectById(foreign.getId())).thenReturn(foreign);
        assertThatThrownBy(() -> entertainService.removeWord("bob", foreign.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有找到");
    }

    // ========== F77 星座配对 ==========

    @Test
    void zodiacRejectsUnknownSignAndSameElementScores95() {
        stubSpace("alice");
        assertThatThrownBy(() -> growthService.zodiac("aries", "pizza"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("星座");

        CoupleGrowthService.ZodiacVO firePair = growthService.zodiac("aries", "leo");
        assertThat(firePair.score()).isEqualTo(95);
        CoupleGrowthService.ZodiacVO reverse = growthService.zodiac("leo", "aries");
        assertThat(reverse.score()).isEqualTo(95);
    }
}
