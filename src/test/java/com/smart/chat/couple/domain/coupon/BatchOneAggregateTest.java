package com.smart.chat.couple.domain.coupon;

import com.smart.chat.couple.domain.RuleViolation;
import com.smart.chat.couple.domain.comfort.ComfortRequest;
import com.smart.chat.couple.domain.safeword.Safeword;
import com.smart.chat.couple.domain.safeword.SafewordUse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 批次一三个聚合的规则与话术（纯领域，不起 Spring）。
 * 话术逐条断言原话，是为了让「搬规则」这件事可证伪：搬错一句话这里就红。
 */
class BatchOneAggregateTest {

    @Test
    void couponTitleIsRequiredAndCapped() {
        assertThatThrownBy(() -> WishCoupon.grant(null, "alice", 99)).isInstanceOf(RuleViolation.class)
                .hasMessage("券面写点什么愿望吧");
        assertThatThrownBy(() -> WishCoupon.grant("  ", "alice", 99)).isInstanceOf(RuleViolation.class)
                .hasMessage("券面写点什么愿望吧");
        assertThatThrownBy(() -> WishCoupon.grant("愿".repeat(81), "alice", 99)).isInstanceOf(RuleViolation.class)
                .hasMessage("最多 80 个字，心意不在字数");
        assertThat(WishCoupon.grant("  陪我去看海  ", "alice", 99).title()).isEqualTo("陪我去看海");
    }

    @Test
    void issuingCostsTenPointsAndSaysWhereToEarn() {
        assertThatThrownBy(() -> WishCoupon.grant("看海", "alice", 9))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("发一张愿望券要 10 分，你只有 9 分——先去好事簿记一笔 TA 为你做过的事吧");
        assertThat(WishCoupon.grant("看海", "alice", 10).isOpen()).isTrue();
    }

    @Test
    void couponUseIsOneWay() {
        WishCoupon coupon = WishCoupon.grant("看海", "alice", 20);
        coupon.useBy("bob", 123L);
        assertThat(coupon.isOpen()).isFalse();
        assertThat(coupon.usedBy()).isEqualTo("bob");
        assertThat(coupon.usedAt()).isEqualTo(123L);
        assertThatThrownBy(() -> coupon.useBy("bob", 456L)).isInstanceOf(RuleViolation.class)
                .hasMessage("这张券已经核销过了");
    }

    @Test
    void couponKnowsWhoItWasGivenTo() {
        WishCoupon fromAlice = WishCoupon.grant("看海", "alice", 20);
        assertThat(fromAlice.addressedTo("bob")).isTrue();
        assertThat(fromAlice.addressedTo("alice")).isFalse();
    }

    @Test
    void safewordMustExistToBeShouted() {
        assertThatThrownBy(() -> Safeword.agree("  ", null)).isInstanceOf(RuleViolation.class)
                .hasMessage("暂停词总得有个词");
        assertThatThrownBy(() -> Safeword.agree("词".repeat(21), null)).isInstanceOf(RuleViolation.class)
                .hasMessage("安全词最多 20 字");
        assertThatThrownBy(() -> Safeword.agree("暂停", "说".repeat(61))).isInstanceOf(RuleViolation.class)
                .hasMessage("用了之后希望最多 60 字");
        assertThatThrownBy(() -> Safeword.requireAgreed(null)).isInstanceOf(RuleViolation.class)
                .hasMessage("先约一个安全词，才喊得出口 🛑");
        Safeword ok = Safeword.agree(" 暂停 ", "先各自十分钟");
        assertThat(ok.word()).isEqualTo("暂停");
        assertThat(ok.note()).isEqualTo("先各自十分钟");
    }

    @Test
    void onlyOnePausePerDayAndOnlyTheShouterReflects() {
        SafewordUse mine = SafewordUse.shout("u1", "2026-10-04", "alice");
        assertThatThrownBy(() -> SafewordUse.assertNotAlreadyToday(mine)).isInstanceOf(RuleViolation.class)
                .hasMessage("今天已经记过一次暂停了，别把安全词用成口头禅");
        SafewordUse.assertNotAlreadyToday(null);

        assertThatThrownBy(() -> mine.reflectBy("bob", "我想了想")).isInstanceOf(RuleViolation.class)
                .hasMessage("那次是 TA 喊的停，复盘要 TA 自己写 📝");
        assertThatThrownBy(() -> mine.reflectBy("alice", "  ")).isInstanceOf(RuleViolation.class)
                .hasMessage("复盘写一句：当时卡在哪、后来怎么接着聊的");
        assertThatThrownBy(() -> mine.reflectBy("alice", "字".repeat(61))).isInstanceOf(RuleViolation.class)
                .hasMessage("复盘最多 60 字");
        assertThat(mine.alreadyReflected()).isFalse();
        mine.reflectBy("alice", "当时卡在翻旧账，后来约好先说事实");
        assertThat(mine.alreadyReflected()).isTrue();
    }

    @Test
    void comfortAcceptsOnlyTheFiveFeelings() {
        assertThat(ComfortRequest.FEELINGS).containsExactlyInAnyOrder("SAD", "WRONGED", "TIRED", "ANXIOUS", "EMO");
        assertThatThrownBy(() -> ComfortRequest.ask("alice", "HUNGRY")).isInstanceOf(RuleViolation.class)
                .hasMessage("感受只能是 难过/委屈/累/焦虑/emo 哦");
        assertThat(ComfortRequest.ask("alice", ComfortRequest.SAD).askedBy("alice")).isTrue();
        // 取话术卡的入口文案不同：一个是教用户选，一个是说明取不到
        assertThatThrownBy(() -> ComfortRequest.requireKnown("HUNGRY")).isInstanceOf(RuleViolation.class)
                .hasMessage("不认识这种感受哦");
    }

    @Test
    void comfortNoteMustBeARealSentenceWithinHundred() {
        assertThatThrownBy(() -> ComfortRequest.requireNote(null)).isInstanceOf(RuleViolation.class)
                .hasMessage("写一句 100 字以内的话，把抱抱送过去");
        assertThatThrownBy(() -> ComfortRequest.requireNote("抱".repeat(101))).isInstanceOf(RuleViolation.class)
                .hasMessage("写一句 100 字以内的话，把抱抱送过去");
        ComfortRequest request = ComfortRequest.restore("c1", "alice", "SAD", null, null);
        assertThat(request.isHeld()).isFalse();
        request.hold(ComfortRequest.requireNote("  我在，晚点聊  "), 777L);
        assertThat(request.isHeld()).isTrue();
        assertThat(request.note()).isEqualTo("我在，晚点聊");
        assertThat(request.heldAt()).isEqualTo(777L);
    }
}
