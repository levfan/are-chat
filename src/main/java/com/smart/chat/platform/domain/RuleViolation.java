package com.smart.chat.platform.domain;

/**
 * 领域规则被违反（形状同 {@code couple.domain.RuleViolation}，各上下文各持一份，不跨上下文共用）。
 * <p>
 * 消息就是用户看到的那句原话（「公告内容不能为空」「公告不存在」），话术属于领域；
 * application 只把它翻译成 {@code BusinessException(400/404, message)}，HTTP 契约一字不变。
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
