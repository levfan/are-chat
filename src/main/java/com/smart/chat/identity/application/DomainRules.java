package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.RuleViolation;
import com.smart.chat.sharedkernel.web.BusinessException;

import java.util.function.Supplier;

/**
 * 领域违规到对外异常的翻译器（identity 版，手法同 {@code couple.application.DomainRules}）。
 * <p>
 * {@link RuleViolation} 的消息就是用户看到的那句原话，状态码也是产品口径的一部分
 * （401 登录失败 / 403 权限与状态闸门 / 409 唯一性与状态机冲突 / 429 验证码节流），
 * 所以这里<b>原样搬</b>，绝不重写文案、也绝不重新推算状态码。
 */
final class DomainRules {

    private DomainRules() {
    }

    static <T> T rule(Supplier<T> body) {
        try {
            return body.get();
        } catch (RuleViolation e) {
            throw translate(e);
        }
    }

    static void guard(Runnable body) {
        try {
            body.run();
        } catch (RuleViolation e) {
            throw translate(e);
        }
    }

    /** 直接把已经构造好的违规翻成对外异常（用于领域返回的结论对象，如验证码的 Outcome） */
    static BusinessException translate(RuleViolation e) {
        return new BusinessException(e.code(), e.getMessage());
    }
}
