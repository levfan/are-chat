package com.smart.chat.couple;

import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 体验优化单测：今日看点聚合、年度热力日历分级、生日前 3 天预告（F92/F95/F96）。
 */
@ExtendWith(MockitoExtension.class)
class CoupleTodayServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @Mock
    private CoupleChallengeMapper challengeMapper;

    @Mock
    private CoupleTruthMapper truthMapper;

    @Mock
    private CoupleMoodMapper moodMapper;

    @Mock
    private CouplePassbookMapper passbookMapper;

    @Mock
    private CoupleHundredMapper hundredMapper;

    @Mock
    private CoupleHundredCheckinMapper checkinMapper;

    @Mock
    private CoupleCapsuleMapper capsuleMapper;

    @Mock
    private CoupleSweetAlarmMapper alarmMapper;

    @Mock
    private CoupleMissExpressMapper missMapper;

    @Mock
    private CoupleSurpriseService surpriseService;

    @Mock
    private CoupleGardenService gardenService;

    @Mock
    private UserProfileMapper profileMapper;

    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleTodayService todayService;

    @InjectMocks
    private CoupleSurpriseJob surpriseJob;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace() {
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
    }

    // ========== F95 今日看点 ==========

    @Test
    void todayAggregatesChecklistFromRealData() {
        stubSpace();
        String today = LocalDate.now().toString();
        CoupleChallenge challenge = CoupleChallenge.of("s1", today, "一起夸对方三次");
        challenge.setDoneA(true);
        when(challengeMapper.findBySpace("s1")).thenReturn(List.of(challenge));
        CoupleTruth truth = CoupleTruth.of("s1", today, "最想一起去哪？", "alice", "冰岛");
        when(truthMapper.findBySpace("s1")).thenReturn(List.of(truth));
        CoupleMood mood = CoupleMood.of("s1", "alice", today, "HAPPY", null);
        when(moodMapper.findBySpace("s1")).thenReturn(List.of(mood));
        CouplePassbook deposit = CouplePassbook.of("s1", "alice", "给 TA 做了早饭");
        deposit.setDay(today);
        when(passbookMapper.find("s1", "alice", today)).thenReturn(deposit);
        when(hundredMapper.findBySpace("s1")).thenReturn(List.of());
        CoupleCapsule capsule = CoupleCapsule.of("s1", "bob", "alice", "给未来的你",
                LocalDate.now().plusDays(10).toString());
        when(capsuleMapper.findBySpace("s1")).thenReturn(List.of(capsule));

        CoupleTodayService.TodayBoardVO board = todayService.today("alice");

        assertThat(board.challengeDone()).isTrue();
        assertThat(board.truthAnswered()).isTrue();
        assertThat(board.moodLogged()).isTrue();
        assertThat(board.passbookDeposited()).isTrue();
        assertThat(board.hundredChecked()).isFalse();
        assertThat(board.nextCapsuleDay()).isEqualTo(LocalDate.now().plusDays(10).toString());
        assertThat(board.capsuleDaysLeft()).isEqualTo(10);
    }

    // ========== F96 年度热力日历 ==========

    @Test
    void heatmapCountsPerDayAndGradesLevel() {
        stubSpace();
        int year = LocalDate.now().getYear();
        String hot = LocalDate.of(year, 6, 1).toString();
        when(moodMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleMood.of("s1", "alice", hot, "HAPPY", null),
                CoupleMood.of("s1", "bob", hot, "CALM", null)));
        CouplePassbook deposit = CouplePassbook.of("s1", "alice", "散步");
        deposit.setDay(hot);
        when(passbookMapper.findBySpace("s1")).thenReturn(List.of(deposit));
        CoupleChallenge challenge = CoupleChallenge.of("s1", hot, "一起散步");
        when(challengeMapper.findBySpace("s1")).thenReturn(List.of(challenge));
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());
        when(hundredMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleTodayService.HeatmapVO heat = todayService.heatmap("alice", year);

        CoupleTodayService.HeatmapDayVO june1 = heat.days().stream()
                .filter(d -> d.day().equals(hot)).findFirst().orElseThrow();
        assertThat(june1.count()).isEqualTo(4);
        assertThat(june1.level()).isEqualTo(2);
        assertThat(heat.totalActive()).isEqualTo(1);
        // 非目标年份的数据不计数
        assertThat(heat.days()).hasSize(LocalDate.of(year, 12, 31).getDayOfYear());
    }

    @Test
    void heatmapClampsOutOfRangeYearToCurrentYear() {
        stubSpace();
        int currentYear = LocalDate.now().getYear();
        when(moodMapper.findBySpace("s1")).thenReturn(List.of());
        when(passbookMapper.findBySpace("s1")).thenReturn(List.of());
        when(challengeMapper.findBySpace("s1")).thenReturn(List.of());
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());
        when(hundredMapper.findBySpace("s1")).thenReturn(List.of());

        // 超大年份与过老年份都回落到当年，防止内存放大
        assertThat(todayService.heatmap("alice", 999999).year()).isEqualTo(currentYear);
        assertThat(todayService.heatmap("alice", 1999).year()).isEqualTo(currentYear);
        assertThat(todayService.heatmap("alice", null).year()).isEqualTo(currentYear);
    }

    // ========== F92 生日前 3 天预告 ==========

    @Test
    void birthdayEveRemindsPartnerThreeDaysAhead() {
        when(spaceMapper.findAllActive()).thenReturn(List.of(space()));
        String eve = LocalDate.now().plusDays(3).toString();
        UserProfile profile = new UserProfile();
        profile.setBirthday(eve);
        when(profileMapper.selectById("alice")).thenReturn(profile);
        when(profileMapper.selectById("bob")).thenReturn(null);

        surpriseJob.birthdayCards();

        verify(push).pushCoupleEvent(eq("birthday-eve"), eq("system"), eq("bob"), anyString());
    }
}
