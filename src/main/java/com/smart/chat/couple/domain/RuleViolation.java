package com.smart.chat.couple.domain;

/**
 * 领域规则被违反。
 * <p>
 * 消息用的是产品原话（用户看到的就是这句），所以**话术属于领域**，不属于 Controller 或 Service：
 * 「这张券已经核销过了」「复盘要 TA 自己写」这些句子就是统一语言的一部分，改它们等于改规则。
 * application 层只负责把它翻译成对外的 {@code BusinessException(400/404, message)}，HTTP 契约一字不变。
 */
public class RuleViolation extends RuntimeException {

    private final boolean notFound;

    public RuleViolation(String message) {
        this(message, false);
    }

    public RuleViolation(String message, boolean notFound) {
        super(message);
        this.notFound = notFound;
    }

    /** true = 对象找不到（对外 404）；false = 业务闸门（对外 400） */
    public boolean notFound() {
        return notFound;
    }
}
