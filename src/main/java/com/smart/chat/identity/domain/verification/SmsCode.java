package com.smart.chat.identity.domain.verification;

import com.smart.chat.identity.domain.RuleViolation;

/**
 * 手机验证码：6 位数字、5 分钟有效、同号 60 秒内不可重发、最多试错 5 次、用过即作废。
 * <p>
 * 不可变：一次校验产出的「下一步」是新实例（{@link Outcome}），
 * 「谁把这条验证码作废、谁把重试计数存回去」是存储编排，留在 {@code SmsCodeService}；
 * 而<b>该不该拒绝、拒绝时说什么、状态怎么迁移</b>都在这里。
 * <p>
 * 演示环境没有短信网关，验证码由 Service 生成后直接回显给前端，这里只负责发号之后的规则。
 */
public final class SmsCode {

    /** 有效期：5 分钟 */
    public static final long TTL_MS = 5 * 60 * 1000L;
    /** 同号重发间隔：60 秒 */
    public static final long RESEND_INTERVAL_MS = 60 * 1000L;
    /** 一条验证码最多试错次数 */
    public static final int MAX_VERIFY_ATTEMPTS = 5;

    /** 一次校验的结论：通过并作废 / 过期作废 / 试错用满作废 / 答案不对但条目还在 */
    public enum Result { CONSUMED, EXPIRED, ATTEMPTS_EXCEEDED, WRONG_CODE }

    /**
     * 校验结论。
     *
     * @param result  该做什么
     * @param next    仅 {@link Result#WRONG_CODE} 非空：要存回去的新状态（试错次数 +1）
     * @param failure 除 {@link Result#CONSUMED} 外都非空：对外的原话与状态码
     */
    public record Outcome(Result result, SmsCode next, RuleViolation failure) {

        /** 通过时没有失败可抛；其余分支一律抛领域违规 */
        public void raiseIfFailed() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private final String code;
    private final long expiresAt;
    private final long nextSendAt;
    private final int attempts;

    private SmsCode(String code, long expiresAt, long nextSendAt, int attempts) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.nextSendAt = nextSendAt;
        this.attempts = attempts;
    }

    /** 新下发的验证码：有效期与重发窗口都从此刻起算，试错计数归零 */
    public static SmsCode fresh(String digits, long now) {
        return new SmsCode(digits, now + TTL_MS, now + RESEND_INTERVAL_MS, 0);
    }

    /** 重发节流：还没到 60 秒就告诉用户还要等多久（429） */
    public void assertResendAllowed(long now) {
        if (now < nextSendAt) {
            long wait = (nextSendAt - now) / 1000 + 1;
            throw RuleViolation.tooManyRequests("发送太频繁了，" + wait + " 秒后再试");
        }
    }

    /**
     * 校验一次输入。判定顺序照改造前的 {@code verifyAndConsume}，一个字都没动：
     * 先看过期、再看试错次数、最后比内容。
     */
    public Outcome verify(String input, long now) {
        if (now > expiresAt) {
            return new Outcome(Result.EXPIRED, null, RuleViolation.badRequest("验证码已过期，请重新获取"));
        }
        if (attempts >= MAX_VERIFY_ATTEMPTS) {
            return new Outcome(Result.ATTEMPTS_EXCEEDED, null,
                    RuleViolation.tooManyRequests("验证码错误次数过多，请重新获取"));
        }
        if (input == null || !code.equals(input.trim())) {
            SmsCode retried = new SmsCode(code, expiresAt, nextSendAt, attempts + 1);
            return new Outcome(Result.WRONG_CODE, retried, RuleViolation.badRequest("验证码不正确"));
        }
        return new Outcome(Result.CONSUMED, null, null);
    }

    /** 剩余有效秒数（注册接口把它回显给前端做倒计时） */
    public static long ttlSeconds() {
        return TTL_MS / 1000;
    }
}
