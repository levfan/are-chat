package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.RuleViolation;
import com.smart.chat.sharedkernel.web.BusinessException;

import java.util.function.Supplier;

/**
 * 领域违规到对外异常的翻译器：RuleViolation 的消息就是用户看到的那句原话，
 * 这里只决定它是 400 还是 404，绝不重写文案——文案归领域（见 couple.domain.RuleViolation）。
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
        int status = e.status();
        if (status == 0) {
            status = e.notFound() ? 404 : 400;
        }
        return new BusinessException(status, e.getMessage());
    }
}
