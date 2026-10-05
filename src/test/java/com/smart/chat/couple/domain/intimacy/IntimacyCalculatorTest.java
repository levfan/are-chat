package com.smart.chat.couple.domain.intimacy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 心动值计算器的纯领域单测——不起 Spring、不用 Mockito，因为领域层本来就不该需要这些。
 * <p>
 * 与 {@code CoupleIntimacyTest}（打 Service + mock 端口）的分工：那条锁「端到端算出来的分对不对」，
 * 这一条锁「规则本身」：权重、七级边界、进度封顶、非法供数。改阈值时这里会先红，逼着回答
 * 「会不会让某对情侣当场掉级」。
 * <p>
 * 2026-10-05 二轮裁剪把六项供数换成五项（在一起天数 / 打卡天数 / 历史最长连续 /
 * 双方都答完每日一问的天数 / 已实现愿望数），阈值同步重标——依据见
 * {@code docs/adr/0010-couple-trim-to-v8-features.md} 第 3 条。
 */
class IntimacyCalculatorTest {

    @Test
    void fiveItemsAreWeightedAsProductDecided() {
        // 每项各 1 → 1×1 + 1×2 + 1×3 + 1×3 + 1×5 = 14
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(1, 1, 1, 1, 1))).isEqualTo(14);
        // 实现的愿望最贵（它意味着真的为对方做了什么）：1 个 = 5 分
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 0, 0, 0, 1))).isEqualTo(5);
        // 答完一题与最长连续同为 ×3；打卡一天 ×2；在一起一天打底 ×1
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 0, 0, 1, 0))).isEqualTo(3);
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 0, 1, 0, 0))).isEqualTo(3);
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(0, 1, 0, 0, 0))).isEqualTo(2);
        assertThat(IntimacyCalculator.scoreOf(new IntimacySource(1, 0, 0, 0, 0))).isEqualTo(1);
    }

    @Test
    void ladderMatchesTheSevenLevelsAndTheirThresholds() {
        assertThat(IntimacyCalculator.levelOf(0).level()).isEqualTo(1);
        assertThat(IntimacyCalculator.levelOf(59).level()).isEqualTo(1);
        assertThat(IntimacyCalculator.levelOf(60).level()).isEqualTo(2);
        assertThat(IntimacyCalculator.levelOf(149).level()).isEqualTo(2);
        assertThat(IntimacyCalculator.levelOf(150).level()).isEqualTo(3);
        assertThat(IntimacyCalculator.levelOf(260).level()).isEqualTo(4);
        assertThat(IntimacyCalculator.levelOf(400).level()).isEqualTo(5);
        assertThat(IntimacyCalculator.levelOf(560).level()).isEqualTo(6);
        assertThat(IntimacyCalculator.levelOf(759).level()).isEqualTo(6);
        assertThat(IntimacyCalculator.levelOf(760).level()).isEqualTo(7);
    }

    @Test
    void titlesAndIconsStayPairedWithLevel() {
        assertThat(IntimacyCalculator.levelOf(0).title()).isEqualTo("怦然心动");
        assertThat(IntimacyCalculator.levelOf(60).title()).isEqualTo("心动初启");
        assertThat(IntimacyCalculator.levelOf(150).title()).isEqualTo("甜甜热恋");
        assertThat(IntimacyCalculator.levelOf(260).title()).isEqualTo("形影不离");
        assertThat(IntimacyCalculator.levelOf(400).title()).isEqualTo("心有灵犀");
        assertThat(IntimacyCalculator.levelOf(560).title()).isEqualTo("相依相伴");
        assertThat(IntimacyCalculator.levelOf(760).title()).isEqualTo("相守一生");
        assertThat(IntimacyCalculator.levelOf(760).icon()).isEqualTo("💍");
    }

    @Test
    void nextLevelAtIsNullOnlyAtMaxLevel() {
        assertThat(IntimacyCalculator.levelOf(0).nextLevelAt()).isEqualTo(60);
        assertThat(IntimacyCalculator.levelOf(60).nextLevelAt()).isEqualTo(150);
        assertThat(IntimacyCalculator.levelOf(759).nextLevelAt()).isEqualTo(760);
        assertThat(IntimacyCalculator.levelOf(760).nextLevelAt()).isNull();
    }

    @Test
    void progressIsInterpolatedAndCappedBelowHundred() {
        assertThat(IntimacyCalculator.levelOf(0).progress()).isZero();
        // L2 区间 60~150（跨度 90），105 分正好走一半
        assertThat(IntimacyCalculator.levelOf(105).progress()).isEqualTo(50);
        // 差一级也算没到：L2 的 149 分不能显示 100%
        assertThat(IntimacyCalculator.levelOf(149).progress()).isEqualTo(98);
        assertThat(IntimacyCalculator.levelOf(760).progress()).isEqualTo(100);
    }

    @Test
    void evaluateCarriesTheSourceThroughForDisplay() {
        IntimacySource source = new IntimacySource(30, 25, 20, 18, 3);
        IntimacyCalculator.Intimacy result = IntimacyCalculator.evaluate(source);
        // 30×1 + 25×2 + 20×3 + 18×3 + 3×5 = 30+50+60+54+15
        assertThat(result.score()).isEqualTo(209);
        assertThat(result.source()).isEqualTo(source);
        assertThat(result.level().level()).isEqualTo(3);
    }

    @Test
    void hundredDaysCoupleDoesNotTopOutYet() {
        // 定标承诺：满打满算在一起 100 天、天天答题的一对应该落在 L6 门口，不到 L7——
        // L7 要留给一年以上的长期关系，否则百日回顾那天称号已经封顶，后面就没有盼头了
        IntimacySource hundredDays = new IntimacySource(100, 90, 60, 88, 6);
        // 100×1 + 90×2 + 60×3 + 88×3 + 6×5 = 100+180+180+264+30 = 754，差 6 分封顶
        assertThat(IntimacyCalculator.scoreOf(hundredDays)).isEqualTo(754);
        assertThat(IntimacyCalculator.levelOf(IntimacyCalculator.scoreOf(hundredDays)).level()).isEqualTo(6);
    }

    @Test
    void negativeSourceIsRejectedInsteadOfSilentlyLoweringScore() {
        assertThatThrownBy(() -> new IntimacySource(-1, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("daysTogether");
        assertThatThrownBy(() -> new IntimacySource(0, 0, 0, 0, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wishFulfilled");
    }

    @Test
    void emptySourceIsLevelOneNotLevelZero() {
        IntimacyCalculator.IntimacyLevel level =
                IntimacyCalculator.levelOf(IntimacyCalculator.scoreOf(IntimacySource.empty()));
        assertThat(level.level()).isEqualTo(1);
        assertThat(level.title()).isEqualTo("怦然心动");
    }
}
