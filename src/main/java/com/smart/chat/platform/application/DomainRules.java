package com.smart.chat.platform.application;

import com.smart.chat.platform.domain.RuleViolation;
import com.smart.chat.sharedkernel.web.BusinessException;

import java.util.function.Supplier;

/**
 * 领域违规到对外异常的翻译器：RuleViolation 的消息就是用户看到的那句原话，
 * 这里只决定它是 400 还是 404，绝不重写文案——文案归领域（见 platform.domain.RuleViolation）。
 * 形状与 {@code couple.application.DomainRules} 一致，platform 各持一份（那是包私有类，不跨包共用）。
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

    private static BusinessException translate(RuleViolation e) {
        return new BusinessException(e.notFound() ? 404 : 400, e.getMessage());
    }
}
