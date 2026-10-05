package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.invite.InviteRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.streak.StreakDays;
import com.smart.chat.couple.domain.wish.WishRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.messaging.domain.FriendshipChecker;
import com.smart.chat.messaging.domain.PeerProfileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 心动值（{@code GET /api/couple/intimacy}）算式单测。
 * <p>
 * 二轮裁剪后五项供数必须全部来自现役功能：在一起天数、累计打卡天数、历史最长连续、
 * 双方都答完每日一问的天数、已实现愿望数。这里锁的就是「取数口径接对了没有」——
 * 权重与级差本身由 {@code IntimacyCalculatorTest} 锁，两分工不重叠。
 */
@ExtendWith(MockitoExtension.class)
class CoupleIntimacyTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private WishRepository wishRepository;
    @Mock
    private CoupleStreakService streakService;
    @Mock
    private CoupleQuestionService questionService;
    @Mock
    private FriendshipChecker friendships;
    @Mock
    private PeerProfileReader profiles;
    @Mock
    private AccountDirectory accounts;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleService service;

    private static final String DAY = LocalDate.now().toString();

    /** 在一起 30 天的空间：纪念日固定在 29 天前，天数含当天。 */
    private CoupleSpace space;

    @BeforeEach
    void setUp() {
        space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                LocalDate.now().minusDays(29).atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli(),
                LocalDate.now().minusDays(29).toString(), null, null, null, "classic", null);
        lenient().when(spaceRepository.findActiveByMember(any())).thenReturn(Optional.of(space));
        lenient().when(wishRepository.countFulfilled("s1")).thenReturn(0L);
        lenient().when(questionService.bothAnsweredDays("s1")).thenReturn(0L);
        lenient().when(streakService.streakOf("s1")).thenReturn(StreakDays.of(List.of(), LocalDate.now()));
    }

    private static String daysAgo(int back) {
        return LocalDate.now().minusDays(back).toString();
    }

    @Test
    void scoreComesOnlyFromLiveFeatures() {
        // 今天 + 昨天 + 前天连着，另有一天下在上个星期：累计 4 天、最长 3 天
        when(streakService.streakOf("s1")).thenReturn(StreakDays.of(
                List.of(DAY, daysAgo(1), daysAgo(2), daysAgo(6)), LocalDate.now()));
        when(questionService.bothAnsweredDays("s1")).thenReturn(18L);
        when(wishRepository.countFulfilled("s1")).thenReturn(3L);

        CoupleService.IntimacyVO vo = service.intimacy("alice");
        CoupleService.IntimacyBreakdown d = vo.breakdown();

        assertThat(d.daysTogether()).isEqualTo(30);
        assertThat(d.checkinDays()).isEqualTo(4);
        assertThat(d.longestStreak()).isEqualTo(3);
        assertThat(d.answerDays()).isEqualTo(18);
        assertThat(d.wishFulfilled()).isEqualTo(3);
        // 30×1 + 4×2 + 3×3 + 18×3 + 3×5 = 30+8+9+54+15 = 116 → L2
        assertThat(vo.score()).isEqualTo(116);
        assertThat(vo.level()).isEqualTo(2);
        assertThat(vo.title()).isEqualTo("心动初启");
        assertThat(vo.nextLevelAt()).isEqualTo(150);
        assertThat(vo.levelProgress()).isEqualTo(62);
    }

    @Test
    void longestStreakCountsEvenAfterBreakSoLevelsNeverRollBack() {
        // 断签后只剩今天一天：累计打卡 5 天、最长仍是历史的 3 天——等级不能因为断签往下掉
        when(streakService.streakOf("s1")).thenReturn(StreakDays.of(
                List.of(DAY, daysAgo(10), daysAgo(11), daysAgo(12), daysAgo(20)), LocalDate.now()));
        when(questionService.bothAnsweredDays("s1")).thenReturn(5L);

        CoupleService.IntimacyVO vo = service.intimacy("alice");

        assertThat(vo.breakdown().checkinDays()).isEqualTo(5);
        assertThat(vo.breakdown().longestStreak()).isEqualTo(3);
        // 30 + 5×2 + 3×3 + 5×3 = 30+10+9+15 = 64 → 仍在 L2，没退回 L1
        assertThat(vo.score()).isEqualTo(64);
        assertThat(vo.level()).isEqualTo(2);
    }

    @Test
    void oneSidedAnswerDayNeverReachesIntimacy() {
        // 双方都答完的天数由 CoupleQuestionService 算（一人答完不算），这里锁的是心动值不自己另算一套
        when(questionService.bothAnsweredDays("s1")).thenReturn(0L);

        CoupleService.IntimacyVO vo = service.intimacy("alice");

        assertThat(vo.breakdown().answerDays()).isZero();
        // 只剩在一起天数：30×1 = 30 → L1
        assertThat(vo.score()).isEqualTo(30);
        assertThat(vo.level()).isEqualTo(1);
    }

    @Test
    void ladderUsesTheRecalibratedThresholds() {
        // 答题 20 天 + 天天打卡 20 天 + 最长 20 + 在一起 30 + 实现 0 → 30+40+60+60 = 190 → L3
        when(streakService.streakOf("s1")).thenReturn(StreakDays.of(
                java.util.stream.IntStream.range(0, 20).mapToObj(i -> daysAgo(i)).toList(), LocalDate.now()));
        when(questionService.bothAnsweredDays("s1")).thenReturn(20L);
        assertThat(service.intimacy("alice").level()).isEqualTo(3);

        // 满级：把愿望数堆到 760 分以上，必须封顶且进度 100
        when(wishRepository.countFulfilled("s1")).thenReturn(140L);
        CoupleService.IntimacyVO top = service.intimacy("alice");
        assertThat(top.level()).isEqualTo(7);
        assertThat(top.title()).isEqualTo("相守一生");
        assertThat(top.nextLevelAt()).isNull();
        assertThat(top.levelProgress()).isEqualTo(100);
    }
}
