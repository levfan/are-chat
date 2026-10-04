package com.smart.chat.couple.domain.streak;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 连续打卡值对象的口径守卫：这两条产品规则一旦漂移，用户的外观就会被没收或者永远解不了锁，
 * 所以逐条锁死而不是靠看板截图发现。
 */
class BondStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    private BondStreak streakOf(String... days) {
        return BondStreak.of(List.of(days), TODAY);
    }

    @Test
    void todayNotCheckedYetDoesNotCountAsBroken() {
        BondStreak streak = streakOf("2026-10-02", "2026-10-03", "2026-10-04");
        assertThat(streak.checkedToday()).isFalse();
        assertThat(streak.missedYesterday()).isFalse();
        assertThat(streak.currentStreak()).isEqualTo(3);
    }

    @Test
    void missingYesterdayBreaksTheRun() {
        BondStreak streak = streakOf("2026-10-01", "2026-10-02");
        assertThat(streak.missedYesterday()).isTrue();
        assertThat(streak.currentStreak()).isZero();
        assertThat(streak.longestStreak()).isEqualTo(2);
    }

    @Test
    void longestIsTheHistoricalPeakSoUnlocksNeverRollBack() {
        // 九月连着 4 天、十月只连了 2 天：当前 2，峰值 4 —— 气泡不能因为最近断了就收回去
        BondStreak streak = streakOf("2026-09-20", "2026-09-21", "2026-09-22", "2026-09-23",
                "2026-10-04", "2026-10-05");
        assertThat(streak.currentStreak()).isEqualTo(2);
        assertThat(streak.longestStreak()).isEqualTo(4);
        assertThat(streak.unlocked("bubble")).isTrue();
        assertThat(streak.unlocked("background")).isFalse();
        assertThat(streak.unlockedTierKeys()).containsExactly("bubble");
        assertThat(streak.nextTier()).isEqualTo(StreakTier.BACKGROUND);
    }

    @Test
    void unlockedDayIsTheMomentTheRunFirstReachedTheTier() {
        BondStreak streak = streakOf("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04");
        assertThat(streak.unlockedAt(StreakTier.BUBBLE)).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(streak.unlockedAt(StreakTier.BACKGROUND)).isNull();
    }

    @Test
    void nextTierAndDistanceUseTheCurrentRun() {
        BondStreak streak = streakOf("2026-10-04", "2026-10-05");
        assertThat(streak.nextTier()).isEqualTo(StreakTier.BUBBLE);
        assertThat(streak.daysToNextTier()).isEqualTo(1);
        assertThat(streak.unlockedTierKeys()).isEmpty();
    }

    @Test
    void tiersCrossedOnlyFiresWhenActuallyPassingATier() {
        assertThat(BondStreak.tiersCrossed(2, 3)).containsExactly(StreakTier.BUBBLE);
        // 断签后重新爬到 3 天：峰值没涨，就不该再庆祝一次气泡
        assertThat(BondStreak.tiersCrossed(3, 3)).isEmpty();
        assertThat(BondStreak.tiersCrossed(3, 8)).containsExactly(StreakTier.BACKGROUND);
        assertThat(BondStreak.tiersCrossed(99, 100)).containsExactly(StreakTier.EASTER_EGG);
    }

    @Test
    void unparsableDaysAreIgnoredInsteadOfCrashingTheBoard() {
        BondStreak streak = BondStreak.of(java.util.Arrays.asList("2026-10-05", "not-a-day", "", null), TODAY);
        assertThat(streak.confirmedDays()).isEqualTo(1);
        assertThat(streak.checkedToday()).isTrue();
    }

    @Test
    void tierKeysMatchTheFrontendHookNames() {
        assertThat(StreakTier.ofKey("nickname-glow")).isEqualTo(StreakTier.NICKNAME_GLOW);
        assertThat(StreakTier.ofKey("nope")).isNull();
        assertThat(StreakTier.values()).hasSize(7);
    }
}
