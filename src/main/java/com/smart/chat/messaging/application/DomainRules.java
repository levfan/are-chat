package com.smart.chat.messaging.application;

import com.smart.chat.messaging.domain.RuleViolation;
import com.smart.chat.sharedkernel.web.BusinessException;

import java.util.function.Supplier;

/**
 * 领域违规到对外异常的翻译器：{@code RuleViolation} 的消息就是用户看到的那句原话，
 * 这里只把它带上领域给的那个 code（400/403/404/409）转成 {@code BusinessException}，绝不重写文案——
 * 文案归领域（见 {@code com.smart.chat.messaging.domain.RuleViolation}）。
 * <p>
 * 与 {@code couple.application.DomainRules} 同形状，差别有两条：messaging 的对外契约有四种状态码
 * （couple 只有 400/404，所以那边用 notFound 布尔位），以及这里是 public——
 * messaging 的 api 层（ProfileController 的资料卡可见性）也要做同一份翻译，
 * 再抄一个翻译器就会有两份文案口径。
 */
public final class DomainRules {

    private DomainRules() {
    }

    public static <T> T rule(Supplier<T> body) {
        try {
            return body.get();
        } catch (RuleViolation e) {
            throw translate(e);
        }
    }

    public static void guard(Runnable body) {
        try {
            body.run();
        } catch (RuleViolation e) {
            throw translate(e);
        }
    }

    private static BusinessException translate(RuleViolation e) {
        return new BusinessException(e.code(), e.getMessage());
    }
}
