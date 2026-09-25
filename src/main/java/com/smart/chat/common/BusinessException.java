package com.smart.chat.common;

/**
 * 业务异常：message 沿用老项目的整活文案，code 同时作为 HTTP 状态码返回。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
