package com.smart.chat.identity.domain.verification;

import com.smart.chat.identity.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 手机验证码规则：重发节流、有效期、试错计数、用过即作废。
 * 判定顺序（先过期、再次数、最后比内容）与文案都是改造前 SmsCodeService 的口径。
 */
class SmsCodeTest {

    private static final long T0 = 1_000_000L;

    private SmsCode fresh() {
        return SmsCode.fresh("123456", T0);
    }

    @Test
    void freshCodeRunsOutAfterFiveMinutes() {
        SmsCode code = fresh();

        assertThat(code.verify("123456", T0 + SmsCode.TTL_MS).result()).isEqualTo(SmsCode.Result.CONSUMED);

        SmsCode.Outcome expired = code.verify("123456", T0 + SmsCode.TTL_MS + 1);
        assertThat(expired.result()).isEqualTo(SmsCode.Result.EXPIRED);
        assertThat(expired.next()).as("过期后没有要存回去的新状态，条目要删掉").isNull();
        assertThatThrownBy(expired::raiseIfFailed)
                .isInstanceOf(RuleViolation.class)
                .hasMessage("验证码已过期，请重新获取")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
    }

    @Test
    void resendIsThrottledForSixtySeconds() {
        SmsCode code = fresh();

        assertThatThrownBy(() -> code.assertResendAllowed(T0 + SmsCode.RESEND_INTERVAL_MS - 1000))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("发送太频繁了，2 秒后再试")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(429);

        code.assertResendAllowed(T0 + SmsCode.RESEND_INTERVAL_MS);
    }

    @Test
    void wrongCodeKeepsTheEntryButCountsTheAttempt() {
        SmsCode code = fresh();

        SmsCode.Outcome wrong = code.verify("000000", T0 + 1);
        assertThat(wrong.result()).isEqualTo(SmsCode.Result.WRONG_CODE);
        assertThat(wrong.next()).isNotNull();
        assertThatThrownBy(wrong::raiseIfFailed)
                .isInstanceOf(RuleViolation.class)
                .hasMessage("验证码不正确");

        // 计数累加在 next 里，重发窗口不受影响
        SmsCode retried = wrong.next();
        retried.assertResendAllowed(T0 + SmsCode.RESEND_INTERVAL_MS);
        assertThatThrownBy(() -> retried.assertResendAllowed(T0 + 1))
                .isInstanceOf(RuleViolation.class)
                .hasMessageContaining("发送太频繁了");
    }

    @Test
    void fiveWrongAttemptsBurnsTheCode() {
        SmsCode code = fresh();
        for (int i = 0; i < SmsCode.MAX_VERIFY_ATTEMPTS; i++) {
            SmsCode.Outcome wrong = code.verify("000000", T0 + i + 1);
            assertThat(wrong.result()).isEqualTo(SmsCode.Result.WRONG_CODE);
            code = wrong.next();
        }

        SmsCode.Outcome burned = code.verify("123456", T0 + 100);
        assertThat(burned.result()).as("次数用满后连正确答案也不再放行").isEqualTo(SmsCode.Result.ATTEMPTS_EXCEEDED);
        assertThat(burned.next()).isNull();
        assertThatThrownBy(burned::raiseIfFailed)
                .isInstanceOf(RuleViolation.class)
                .hasMessage("验证码错误次数过多，请重新获取")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(429);
    }

    @Test
    void matchingIsTrimTolerantAndConsumedOnSuccess() {
        SmsCode.Outcome ok = fresh().verify(" 123456 ", T0 + 1);

        assertThat(ok.result()).isEqualTo(SmsCode.Result.CONSUMED);
        assertThat(ok.failure()).isNull();
        ok.raiseIfFailed();
    }

    @Test
    void nullInputCountsAsWrong() {
        assertThat(fresh().verify(null, T0 + 1).result()).isEqualTo(SmsCode.Result.WRONG_CODE);
    }

    @Test
    void ttlSecondsMatchesTheWindow() {
        assertThat(SmsCode.ttlSeconds()).isEqualTo(300L);
    }
}
