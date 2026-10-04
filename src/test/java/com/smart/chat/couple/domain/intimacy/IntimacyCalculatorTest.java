package com.smart.chat.couple.domain.intimacy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 心动值计算器的纯领域单测——不起 Spring、不用 Mockito，因为领域层本来就不该需要这些。
 * <p>
 * 与 {@code CoupleIntimacyTest}（打 Service + mock mapper）的分工：那条锁「端到端算出来的分对不对」，
 * 这一条锁「规则本身」：权重、七级边界、进度封顶、非法供数。改阈值时这里会先红，逼着回答
 * 「会不会让某对情侣当场掉级」。
 */
class IntimacyCalculatorTest {

    @Test
    void sixItemsAreWeightedAsProductDecided() {
        // 每项各 1 → 1×1 + 1×2 + 1×2 + 1×3 + 1×2 + 1×1 = 11
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(1, 1, 1, 1, 1, 1))).isEqualTo(11);
        // 留灯最贵：1 张灯卡 = 3 分，1 件好事 = 2 分，1 天心情 = 1 分
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 0, 0, 1, 0, 0))).isEqualTo(3);
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 0, 1, 0, 0, 0))).isEqualTo(2);
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(1, 0, 0, 0, 0, 0))).isEqualTo(1);
        // 积分按台账原值累加，不再乘系数
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 0, 0, 0, 0, 27))).isEqualTo(27);
    }

    @Test
    void ladderMatchesTheSevenLevelsAndTheirThresholds() {
        assertThat(IntimacyCalculator.levelOf(0).level()).isEqualTo(1);
        assertThat(IntimacyCalculator.levelOf(49).level()).isEqualTo(1);
        assertThat(IntimacyCalculator.levelOf(50).level()).isEqualTo(2);
        assertThat(IntimacyCalculator.levelOf(149).level()).isEqualTo(2);
        assertThat(IntimacyCalculator.levelOf(150).level()).isEqualTo(3);
        assertThat(IntimacyCalculator.levelOf(300).level()).isEqualTo(4);
        assertThat(IntimacyCalculator.levelOf(500).level()).isEqualTo(5);
        assertThat(IntimacyCalculator.levelOf(800).level()).isEqualTo(6);
        assertThat(IntimacyCalculator.levelOf(1299).level()).isEqualTo(6);
        assertThat(IntimacyCalculator.levelOf(1300).level()).isEqualTo(7);
    }

    @Test
    void titlesAndIconsStayPairedWithLevel() {
        assertThat(IntimacyCalculator.levelOf(0).title()).isEqualTo("怦然心动");
        assertThat(IntimacyCalculator.levelOf(50).title()).isEqualTo("心动初启");
        assertThat(IntimacyCalculator.levelOf(150).title()).isEqualTo("甜甜热恋");
        assertThat(IntimacyCalculator.levelOf(300).title()).isEqualTo("形影不离");
        assertThat(IntimacyCalculator.levelOf(500).title()).isEqualTo("心有灵犀");
        assertThat(IntimacyCalculator.levelOf(800).title()).isEqualTo("相依相伴");
        assertThat(IntimacyCalculator.levelOf(1300).title()).isEqualTo("相守一生");
        assertThat(IntimacyCalculator.levelOf(1300).icon()).isEqualTo("💍");
    }

    @Test
    void nextLevelAtIsNullOnlyAtMaxLevel() {
        assertThat(IntimacyCalculator.levelOf(0).nextLevelAt()).isEqualTo(50);
        assertThat(IntimacyCalculator.levelOf(50).nextLevelAt()).isEqualTo(150);
        assertThat(IntimacyCalculator.levelOf(1299).nextLevelAt()).isEqualTo(1300);
        assertThat(IntimacyCalculator.levelOf(1300).nextLevelAt()).isNull();
    }

    @Test
    void progressIsInterpolatedAndCappedBelowHundred() {
        assertThat(IntimacyCalculator.levelOf(0).progress()).isZero();
        // L2 区间 50~150，75 分走了一半
        assertThat(IntimacyCalculator.levelOf(75).progress()).isEqualTo(25);
        // 差一级也算没到：L2 的 149 分不能显示 100%
        assertThat(IntimacyCalculator.levelOf(149).progress()).isEqualTo(99);
        assertThat(IntimacyCalculator.levelOf(1300).progress()).isEqualTo(100);
    }

    @Test
    void evaluateCarriesTheSourceThroughForDisplay() {
        IntimacySource source = new IntimacySource(4, 2, 1, 1, 0, 2);
        IntimacyCalculator.Intimacy result = IntimacyCalculator.evaluate(source);
        assertThat(result.score()).isEqualTo(4 + 4 + 2 + 3 + 0 + 2);
        assertThat(result.source()).isEqualTo(source);
        assertThat(result.level().level()).isEqualTo(1);
    }

    @Test
    void negativeSourceIsRejectedInsteadOfSilentlyLoweringScore() {
        assertThatThrownBy(() -> new IntimacySource(-1, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moodDays");
        assertThatThrownBy(() -> new IntimacySource(0, 0, 0, 0, 0, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pointEarned");
    }

    @Test
    void emptySourceIsLevelOneNotLevelZero() {
        IntimacyCalculator.IntimacyLevel level = IntimacyCalculator.levelOf(IntimacyCalculator.scoreOf(IntimacySource.empty()));
        assertThat(level.level()).isEqualTo(1);
        assertThat(level.title()).isEqualTo("怦然心动");
    }
}
