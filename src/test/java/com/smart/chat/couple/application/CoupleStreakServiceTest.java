package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.streak.StreakTier;
import com.smart.chat.couple.infrastructure.persistence.CoupleActionMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleActionPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleBondDayMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleBondDayPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 连续互动打卡单测：锁的是「落没落行、推没推对事件、积分有没有真扣」，不是 VO 好不好看。
 * 补签的四条闸门都必须能变红，否则等于没有闸门。
 */
@ExtendWith(MockitoExtension.class)
class CoupleStreakServiceTest {

    private static final String SPACE = "s1";
    private static final String TODAY = LocalDate.now().toString();
    private static final String YESTERDAY = LocalDate.now().minusDays(1).toString();
    private static final String TWO_DAYS_AGO = LocalDate.now().minusDays(2).toString();
    private static final String THREE_DAYS_AGO = LocalDate.now().minusDays(3).toString();
    private static final String FOUR_DAYS_AGO = LocalDate.now().minusDays(4).toString();

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private CoupleBondDayMapper bondDayMapper;
    @Mock
    private CoupleActionMapper actionMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleStreakService service;

    /** 内存假表：断言落在真实副作用上。 */
    private final class Bag {
        private final List<CoupleBondDayPO> days = new ArrayList<>();
        private final List<CoupleActionPO> actions = new ArrayList<>();
        private final List<CouplePointLedgerPO> ledger = new ArrayList<>();

        Bag stub() {
            lenient().when(bondDayMapper.findBySpace(SPACE)).thenAnswer(inv -> List.copyOf(days));
            lenient().when(bondDayMapper.find(eq(SPACE), anyString())).thenAnswer(inv -> days.stream()
                    .filter(d -> d.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
            lenient().when(bondDayMapper.insert(any(CoupleBondDayPO.class))).thenAnswer(inv -> {
                days.add(inv.getArgument(0));
                return 1;
            });
            lenient().when(bondDayMapper.countMakeupBetween(eq(SPACE), anyString(), anyString()))
                    .thenAnswer(inv -> days.stream().filter(CoupleBondDayPO::makeupFlag)
                            .filter(d -> d.getDay().compareTo(inv.getArgument(1, String.class)) >= 0
                                    && d.getDay().compareTo(inv.getArgument(2, String.class)) <= 0).count());
            lenient().when(actionMapper.findSince(eq(SPACE), anyLong())).thenAnswer(inv -> {
                long from = inv.getArgument(1, Long.class);
                return actions.stream().filter(a -> a.getCreated() != null && a.getCreated() >= from).toList();
            });
            lenient().when(ledgerMapper.findBySpace(SPACE)).thenAnswer(inv -> List.copyOf(ledger));
            lenient().when(ledgerMapper.insert(any(CouplePointLedgerPO.class))).thenAnswer(inv -> {
                ledger.add(inv.getArgument(0));
                return 1;
            });
            return this;
        }

        void stubSpace(CoupleSpace space) {
            lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
        }

        void checkin(String day, String source, String operator) {
            days.add(CoupleBondDayPO.of(SPACE, day, source, operator));
        }

        void acted(String who, long at) {
            CoupleActionPO action = CoupleActionPO.of(SPACE, who, CoupleActionPO.KIND_HUG);
            action.setCreated(at);
            actions.add(action);
        }

        void earned(String who, int points) {
            ledger.add(CouplePointLedgerPO.of(SPACE, who, CouplePointLedgerPO.TYPE_EARN, "好事簿", points));
        }

        long autoRows() {
            return days.stream().filter(d -> CoupleBondDayPO.SOURCE_AUTO.equals(d.getSource())).count();
        }

        long makeupRows() {
            return days.stream().filter(CoupleBondDayPO::makeupFlag).count();
        }

        long spendRows() {
            return ledger.stream().filter(l -> CouplePointLedgerPO.TYPE_SPEND.equals(l.getType())).count();
        }
    }

    /** 空间是 30 天前建立的：看板自愈补的第 1 天不会串进本轮的连续段。 */
    private CoupleSpace space() {
        return CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE, LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), null, null, null, null, null, null, null);
    }

    @Test
    void oneSidedBondDoesNotCheckInToday() {
        Bag bag = new Bag().stub();
        bag.acted("alice", System.currentTimeMillis());

        service.markTodayAfterAction(space(), "alice");

        assertThat(bag.days).isEmpty();
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void bothSidesBondingTodayChecksInExactlyOnce() {
        Bag bag = new Bag().stub();
        long now = System.currentTimeMillis();
        bag.acted("alice", now);
        bag.acted("bob", now);

        service.markTodayAfterAction(space(), "alice");
        service.markTodayAfterAction(space(), "bob");

        assertThat(bag.autoRows()).isEqualTo(1);
        verify(push, times(1)).pushCoupleEventBoth(eq("streak-checkin"), eq("alice"), eq("alice"), eq("bob"),
                anyString());
    }

    @Test
    void crossingThreeConsecutiveDaysUnlocksTheBubbleOnce() {
        Bag bag = new Bag().stub();
        bag.checkin(TWO_DAYS_AGO, CoupleBondDayPO.SOURCE_AUTO, null);
        bag.checkin(YESTERDAY, CoupleBondDayPO.SOURCE_AUTO, null);
        long now = System.currentTimeMillis();
        bag.acted("alice", now);
        bag.acted("bob", now);

        service.markTodayAfterAction(space(), "alice");

        verify(push).pushCoupleEventBoth(eq("streak-unlocked"), eq("alice"), eq("alice"), eq("bob"),
                contains(StreakTier.BUBBLE.label()));
        verify(push, never()).pushCoupleEventBoth(eq("streak-unlocked"), eq("alice"), eq("alice"), eq("bob"),
                contains(StreakTier.BACKGROUND.label()));
        assertThat(bag.days).hasSize(3);
        assertThat(bag.days.stream().filter(d -> TODAY.equals(d.getDay())).count()).isEqualTo(1);
    }

    @Test
    void makeupSpendsPointsAndGluesTheRunBackTogether() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.earned("alice", 30);
        bag.checkin(TWO_DAYS_AGO, CoupleBondDayPO.SOURCE_AUTO, null);
        bag.checkin(FOUR_DAYS_AGO, CoupleBondDayPO.SOURCE_AUTO, null);

        CoupleStreakService.StreakBoardVO board = service.makeup("alice", THREE_DAYS_AGO);

        assertThat(bag.makeupRows()).isEqualTo(1);
        assertThat(bag.spendRows()).isEqualTo(1);
        CouplePointLedgerPO spend = bag.ledger.stream()
                .filter(l -> CouplePointLedgerPO.TYPE_SPEND.equals(l.getType())).findFirst().orElseThrow();
        assertThat(spend.getPoints()).isEqualTo(20);
        assertThat(spend.getItem()).startsWith("补签 ");
        assertThat(spend.getFromUser()).isEqualTo("alice");
        assertThat(board.longestStreak()).isEqualTo(3);
        verify(push).pushCoupleEventBoth(eq("streak-makeup"), eq("alice"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void makeupGatesRejectAndWriteNothing() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.earned("alice", 100);

        assertThatThrownBy(() -> service.makeup("alice", LocalDate.now().minusDays(10).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能补最近 7 天");
        assertThatThrownBy(() -> service.makeup("alice", TODAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("今天还不能补");
        bag.checkin(YESTERDAY, CoupleBondDayPO.SOURCE_AUTO, null);
        assertThatThrownBy(() -> service.makeup("alice", YESTERDAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经打过卡");
        assertThatThrownBy(() -> service.makeup("alice", "20261001"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM-dd");

        assertThat(bag.days).hasSize(1);
        assertThat(bag.spendRows()).isZero();
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void makeupBlockedWhenBroke() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());

        assertThatThrownBy(() -> service.makeup("alice", YESTERDAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先去好事簿");
        assertThat(bag.spendRows()).isZero();
    }

    @Test
    void monthlyMakeupQuotaIsThree() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.earned("alice", 200);
        bag.checkin(YESTERDAY, CoupleBondDayPO.SOURCE_MAKEUP, "alice");
        bag.checkin(TWO_DAYS_AGO, CoupleBondDayPO.SOURCE_MAKEUP, "alice");
        bag.checkin(THREE_DAYS_AGO, CoupleBondDayPO.SOURCE_MAKEUP, "alice");

        assertThatThrownBy(() -> service.makeup("alice", FOUR_DAYS_AGO))
                .isInstanceOf(BusinessException.class).hasMessageContaining("这个月已经补过 3 次");
        assertThat(bag.makeupRows()).isEqualTo(3);
    }

    @Test
    void boardExposesTiersStripAndMakeupAffordability() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.checkin(YESTERDAY, CoupleBondDayPO.SOURCE_AUTO, null);
        bag.checkin(TWO_DAYS_AGO, CoupleBondDayPO.SOURCE_AUTO, null);
        bag.earned("alice", 5);

        CoupleStreakService.StreakBoardVO board = service.board("alice");

        assertThat(board.currentStreak()).isEqualTo(2);
        assertThat(board.checkedToday()).isFalse();
        assertThat(board.tiers()).hasSize(StreakTier.values().length);
        assertThat(board.tiers().get(0).key()).isEqualTo(StreakTier.BUBBLE.key());
        assertThat(board.tiers().get(0).unlocked()).isFalse();
        assertThat(board.nextTierKey()).isEqualTo(StreakTier.BUBBLE.key());
        assertThat(board.daysToNext()).isEqualTo(1);
        assertThat(board.strip()).hasSize(21);
        assertThat(board.strip().get(board.strip().size() - 1).day()).isEqualTo(TODAY);
        // 差一天就解锁气泡，但 5 分补不起 20 分的签，canMakeup 必须是 false
        assertThat(board.canMakeup()).isFalse();
    }

    @Test
    void boardCanMakeupWhenYesterdayMissedAndPointsEnough() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.checkin(TWO_DAYS_AGO, CoupleBondDayPO.SOURCE_AUTO, null);
        bag.earned("alice", 40);

        CoupleStreakService.StreakBoardVO board = service.board("alice");

        assertThat(board.missedYesterday()).isTrue();
        assertThat(board.canMakeup()).isTrue();
        assertThat(board.makeupLeftThisMonth()).isEqualTo(3);
    }

    @Test
    void togetherDayOneIsCheckedInWithoutAnyBonding() {
        Bag bag = new Bag().stub();
        CoupleSpace fresh = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null, null, null, null, null, null);
        when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(fresh));

        CoupleStreakService.StreakBoardVO board = service.board("alice");

        assertThat(board.confirmedDays()).isEqualTo(1);
        assertThat(board.currentStreak()).isEqualTo(1);
        assertThat(bag.autoRows()).isEqualTo(1);
    }

    @Test
    void noSpaceThrowsTheStandardFourOhFour() {
        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }
}
