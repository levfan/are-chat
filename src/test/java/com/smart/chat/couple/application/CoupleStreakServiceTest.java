package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.streak.StreakDay;
import com.smart.chat.couple.domain.streak.StreakDayRepository;
import com.smart.chat.couple.domain.streak.StreakTier;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 连续互动打卡单测：锁的是「落没落行、推没推对事件」，不是 VO 好不好看。
 * 补签的每条闸门都必须能变红，否则等于没有闸门；打卡一律不花钱
 * （积分台账已随愿望券本一起在下线清单里删除，稀缺性只剩「7 天窗口 + 每月 3 次」两条守）。
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
    private StreakDayRepository streakDayRepository;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleStreakService service;

    /** 内存假表：断言落在真实副作用上。 */
    private final class Bag {
        private final List<StreakDay> days = new ArrayList<>();

        Bag stub() {
            lenient().when(streakDayRepository.findBySpace(SPACE)).thenAnswer(inv -> List.copyOf(days));
            lenient().when(streakDayRepository.find(eq(SPACE), anyString())).thenAnswer(inv -> days.stream()
                    .filter(d -> d.day().equals(inv.getArgument(1))).findFirst());
            lenient().doAnswer(inv -> {
                days.add(inv.getArgument(0));
                return null;
            }).when(streakDayRepository).append(any(StreakDay.class));
            lenient().when(streakDayRepository.countMakeupBetween(eq(SPACE), anyString(), anyString()))
                    .thenAnswer(inv -> days.stream().filter(StreakDay::makeup)
                            .filter(d -> d.day().compareTo(inv.getArgument(1, String.class)) >= 0
                                    && d.day().compareTo(inv.getArgument(2, String.class)) <= 0).count());
            return this;
        }

        Bag stubSpace(CoupleSpace space) {
            lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
            return this;
        }

        void checkin(String day, String source, String operator) {
            days.add(StreakDay.restore("x" + days.size(), SPACE, day, source, operator,
                    System.currentTimeMillis()));
        }

        long autoRows() {
            return days.stream().filter(d -> StreakDay.SOURCE_AUTO.equals(d.source())).count();
        }

        long makeupRows() {
            return days.stream().filter(StreakDay::makeup).count();
        }
    }

    /** 空间是 30 天前建立的：看板自愈补的第 1 天不会串进本轮的连续段。 */
    private CoupleSpace space() {
        return CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                null, null, null, null, "classic", null);
    }

    @Test
    void alreadyCheckedInTodayIsNotWrittenTwice() {
        Bag bag = new Bag().stub();
        bag.checkin(TODAY, StreakDay.SOURCE_AUTO, null);

        service.confirmBothAnswered(space(), "alice");

        assertThat(bag.days).hasSize(1);
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void bothAnsweredChecksInExactlyOnce() {
        Bag bag = new Bag().stub();

        service.confirmBothAnswered(space(), "alice");
        service.confirmBothAnswered(space(), "bob");

        assertThat(bag.autoRows()).isEqualTo(1);
        verify(push, times(1)).pushCoupleEventBoth(eq("streak-checkin"), eq("alice"), eq("alice"), eq("bob"),
                anyString());
    }

    @Test
    void crossingThreeConsecutiveDaysUnlocksTheBubbleOnce() {
        Bag bag = new Bag().stub();
        bag.checkin(TWO_DAYS_AGO, StreakDay.SOURCE_AUTO, null);
        bag.checkin(YESTERDAY, StreakDay.SOURCE_AUTO, null);

        service.confirmBothAnswered(space(), "alice");

        verify(push).pushCoupleEventBoth(eq("streak-unlocked"), eq("alice"), eq("alice"), eq("bob"),
                contains(StreakTier.BUBBLE.label()));
        verify(push, never()).pushCoupleEventBoth(eq("streak-unlocked"), eq("alice"), eq("alice"), eq("bob"),
                contains(StreakTier.BACKGROUND.label()));
        assertThat(bag.days).hasSize(3);
        assertThat(bag.days.stream().filter(d -> TODAY.equals(d.day())).count()).isEqualTo(1);
    }

    @Test
    void makeupGluesTheRunBackTogetherWithoutSpendingAnything() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.checkin(TWO_DAYS_AGO, StreakDay.SOURCE_AUTO, null);
        bag.checkin(FOUR_DAYS_AGO, StreakDay.SOURCE_AUTO, null);

        CoupleStreakService.StreakBoardVO board = service.makeup("alice", THREE_DAYS_AGO);

        assertThat(bag.makeupRows()).isEqualTo(1);
        assertThat(board.longestStreak()).isEqualTo(3);
        verify(push).pushCoupleEventBoth(eq("streak-makeup"), eq("alice"), eq("alice"), eq("bob"),
                contains("本月还能补 2 次"));
    }

    @Test
    void makeupGatesRejectAndWriteNothing() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());

        // 太久了（超出 7 天窗口）
        assertThatThrownBy(() -> service.makeup("alice", LocalDate.now().minusDays(10).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能补最近 7 天");
        // 今天不能补：两个人都答完每日一问就算打卡，不必补
        assertThatThrownBy(() -> service.makeup("alice", TODAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("今天还不能补");
        // 已经打过的日子
        bag.checkin(YESTERDAY, StreakDay.SOURCE_AUTO, null);
        assertThatThrownBy(() -> service.makeup("alice", YESTERDAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经打过卡");
        // 日期格式
        assertThatThrownBy(() -> service.makeup("alice", "20261001"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM-dd");

        assertThat(bag.days).hasSize(1);
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void monthlyMakeupQuotaIsThree() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.checkin(YESTERDAY, StreakDay.SOURCE_MAKEUP, "alice");
        bag.checkin(TWO_DAYS_AGO, StreakDay.SOURCE_MAKEUP, "alice");
        bag.checkin(THREE_DAYS_AGO, StreakDay.SOURCE_MAKEUP, "alice");

        assertThatThrownBy(() -> service.makeup("alice", FOUR_DAYS_AGO))
                .isInstanceOf(BusinessException.class).hasMessageContaining("这个月已经补过 3 次");
        assertThat(bag.makeupRows()).isEqualTo(3);
    }

    @Test
    void boardExposesTiersStripAndMakeupAvailability() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.checkin(YESTERDAY, StreakDay.SOURCE_AUTO, null);
        bag.checkin(TWO_DAYS_AGO, StreakDay.SOURCE_AUTO, null);

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
        // 昨天打过、今天还没答完 → 不算断，也就不该放开补签
        assertThat(board.canMakeup()).isFalse();
        assertThat(board.makeupLeftThisMonth()).isEqualTo(3);
        assertThat(board.makeupWindowDays()).isEqualTo(7);
    }

    @Test
    void boardOpensMakeupWhenYesterdayIsMissing() {
        Bag bag = new Bag().stub();
        bag.stubSpace(space());
        bag.checkin(TWO_DAYS_AGO, StreakDay.SOURCE_AUTO, null);

        CoupleStreakService.StreakBoardVO board = service.board("alice");

        assertThat(board.missedYesterday()).isTrue();
        assertThat(board.canMakeup()).isTrue();
    }

    @Test
    void togetherDayOneIsCheckedInWithoutAnyAnswer() {
        Bag bag = new Bag().stub();
        CoupleSpace fresh = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null, null, null, "classic", null);
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
