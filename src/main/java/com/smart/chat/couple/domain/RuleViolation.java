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
    /** 0 = 由 notFound 决定 400/404；非 0 = 这条规则本身带对外状态码（403 归属闸门、409 已处理过） */
    private final int status;

    public RuleViolation(String message) {
        this(message, false);
    }

    public RuleViolation(String message, boolean notFound) {
        super(message);
        this.notFound = notFound;
        this.status = 0;
    }

    private RuleViolation(String message, int status, boolean unused) {
        super(message);
        this.notFound = status == 404;
        this.status = status;
    }

    /** 「不归你动」的闸门：对外 403，话术仍然由领域说了算 */
    public static RuleViolation forbidden(String message) {
        return new RuleViolation(message, 403, true);
    }

    /** 这件事已经发生过了（重复点、状态已推进）：对外 409 */
    public static RuleViolation conflict(String message) {
        return new RuleViolation(message, 409, true);
    }

    /** 找不到这个对象：对外 404 */
    public static RuleViolation notFound(String message) {
        return new RuleViolation(message, 404, true);
    }

    /** true = 对象找不到（对外 404）；false = 业务闸门（对外 400） */
    public boolean notFound() {
        return notFound;
    }

    /** 显式指定的对外状态码；0 表示没指定 */
    public int status() {
        return status;
    }
}
