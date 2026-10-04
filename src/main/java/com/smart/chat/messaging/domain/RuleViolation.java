package com.smart.chat.messaging.domain;

/**
 * 领域规则被违反。
 * <p>
 * 消息就是用户看到的那句原话，所以**话术属于领域**：「还不是好友，先加个好友吧」「这条消息已经撤回过了」
 * 这些句子本身就是统一语言的一部分，改它们等于改规则。application 只负责把它翻译成对外的
 * {@code BusinessException(code, message)}，HTTP 契约一字不变（见 docs/ddd/05-tactical-playbook.md 第三节）。
 * <p>
 * 与 {@code couple.domain.RuleViolation}（notFound 布尔位）不同，这里带 code：messaging 的闸门
 * 实测有 400/403/404/409 四档（拉黑是 403、状态机冲突是 409、归属找不到是 404），
 * 而 code 会经 {@code GlobalExceptionHandler} 直接变成 HTTP 状态码，是对外契约，不能压成两档。
 */
public class RuleViolation extends RuntimeException {

    private final int code;

    private RuleViolation(String message, int code) {
        super(message);
        this.code = code;
    }

    /** 业务闸门（默认档）：400 */
    public static RuleViolation of(String message) {
        return new RuleViolation(message, 400);
    }

    /** 对象找不到：404 */
    public static RuleViolation notFound(String message) {
        return new RuleViolation(message, 404);
    }

    /** 越权/归属不符：403 */
    public static RuleViolation forbidden(String message) {
        return new RuleViolation(message, 403);
    }

    /** 状态机冲突（已经处理过、已经撤回过）：409 */
    public static RuleViolation conflict(String message) {
        return new RuleViolation(message, 409);
    }

    public int code() {
        return code;
    }
}
