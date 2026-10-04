package com.smart.chat.filestorage.domain;

/**
 * 领域规则被违反（形状同 {@code couple.domain.RuleViolation}，但各上下文各自持有一份，不跨上下文共用）。
 * <p>
 * 消息就是用户看到的那句产品原话（「不允许上传 .exe 类型的文件」「非法的文件路径：…」），
 * 所以**话术属于领域**；application 只负责把它翻译成 {@code BusinessException(400/404, message)}，
 * HTTP 契约与文案一字不变。
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

    /** true = 找不到（对外 404）；false = 业务闸门（对外 400） */
    public boolean notFound() {
        return notFound;
    }
}
