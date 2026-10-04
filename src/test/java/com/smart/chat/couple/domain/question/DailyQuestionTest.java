package com.smart.chat.couple.domain.question;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 每日一问的互看闸门：**谁没答就看不到 TA 的答案**。
 * 这条规则一旦被"顺手优化"成先答的人能偷看，答题的动力就没了，所以单独钉住。
 */
class DailyQuestionTest {

    @Test
    void partnerAnswerStaysHiddenUntilIAmDoneToo() {
        DailyQuestion view = DailyQuestion.of("2026-10-05", 3, "今天最开心的一件事是什么？", null, "吃到了一碗很好的面");
        assertThat(view.answeredByMe()).isFalse();
        assertThat(view.answeredByPartner()).isTrue();
        assertThat(view.bothAnswered()).isFalse();
        assertThat(view.partnerAnswerText()).isNull();
    }

    @Test
    void bothAnsweredRevealsThePartnerAnswer() {
        DailyQuestion view = DailyQuestion.of("2026-10-05", 3, "今天最开心的一件事是什么？", "见到你", "吃到了一碗很好的面");
        assertThat(view.bothAnswered()).isTrue();
        assertThat(view.partnerAnswerText()).isEqualTo("吃到了一碗很好的面");
        assertThat(view.myAnswerText()).isEqualTo("见到你");
    }

    @Test
    void blankAnswersAreNormalisedToNull() {
        DailyQuestion view = DailyQuestion.of("2026-10-05", 0, "题", "   ", "");
        assertThat(view.answeredByMe()).isFalse();
        assertThat(view.answeredByPartner()).isFalse();
    }

    @Test
    void answerTextIsRequiredAndCapped() {
        assertThat(DailyQuestion.requireAnswerText("  还行  ")).isEqualTo("还行");
        assertThatThrownBy(() -> DailyQuestion.requireAnswerText("   "))
                .isInstanceOf(RuleViolation.class).hasMessage("写一句再交卷呀 📝");
        assertThatThrownBy(() -> DailyQuestion.requireAnswerText("一".repeat(DailyQuestion.ANSWER_MAX + 1)))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("300");
    }
}
