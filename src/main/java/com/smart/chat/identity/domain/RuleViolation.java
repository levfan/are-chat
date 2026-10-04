package com.smart.chat.identity.domain;

/**
 * 领域规则被违反（identity 版，手法同 {@code couple.domain.RuleViolation}）。
 * <p>
 * 消息用的是产品原话——「该手机号已经注册过了，直接登录吧」「该账号已被禁用，联系管理员处理」这些句子
 * 就是统一语言的一部分，所以它们属于领域，不属于 Service 或 Controller。
 * <p>
 * 与 couple 版的差别只有一处：identity 的对外状态码本来就有 400/401/403/404/409/429 好几种
 * （登录失败 401、权限不足 403、重复注册 409、验证码限流 429），而状态码是对外契约，
 * 因此这里直接带上 {@code code}，由 {@code application.DomainRules} 原样翻成
 * {@code BusinessException(code, message)}，<b>不重新推算、不改写文案</b>。
 */
public class RuleViolation extends RuntimeException {

    /** 业务闸门（用户输入不合法） */
    public static final int BAD_REQUEST = 400;
    /** 凭据不成立（登录时不区分「账号不存在」与「密码错误」，避免泄漏账号是否存在） */
    public static final int UNAUTHORIZED = 401;
    /** 角色/状态闸门（不是管理员、账号被禁用或已注销） */
    public static final int FORBIDDEN = 403;
    /** 找不到对象 */
    public static final int NOT_FOUND = 404;
    /** 唯一性与状态机冲突（手机号/用户名已被占用、申请已处理过） */
    public static final int CONFLICT = 409;
    /** 节流（验证码重发太频繁、试错次数用满） */
    public static final int TOO_MANY_REQUESTS = 429;

    private final int code;

    public RuleViolation(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 对外状态码：就是今天线上返回的那个数字，改它等于改 HTTP 契约 */
    public int code() {
        return code;
    }

    public static RuleViolation badRequest(String message) {
        return new RuleViolation(BAD_REQUEST, message);
    }

    public static RuleViolation forbidden(String message) {
        return new RuleViolation(FORBIDDEN, message);
    }

    public static RuleViolation notFound(String message) {
        return new RuleViolation(NOT_FOUND, message);
    }

    public static RuleViolation conflict(String message) {
        return new RuleViolation(CONFLICT, message);
    }

    public static RuleViolation tooManyRequests(String message) {
        return new RuleViolation(TOO_MANY_REQUESTS, message);
    }
}
