package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 手机验证码（演示环境无短信网关：验证码在响应里回显，并写入日志）。
 * 规则：6 位数字、5 分钟有效、同号 60 秒内不可重发、最多试错 5 次、用过即作废。
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    public static final long TTL_MS = 5 * 60 * 1000L;
    public static final long RESEND_INTERVAL_MS = 60 * 1000L;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private record CodeEntry(String code, long expiresAt, long nextSendAt, int attempts) {
    }

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** 生成并下发验证码，返回验证码本身（演示环境直接回显给前端） */
    public String issue(String phone) {
        long now = System.currentTimeMillis();
        CodeEntry existing = codes.get(phone);
        if (existing != null && now < existing.nextSendAt()) {
            long wait = (existing.nextSendAt() - now) / 1000 + 1;
            throw new BusinessException(429, "发送太频繁了，" + wait + " 秒后再试");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(phone, new CodeEntry(code, now + TTL_MS, now + RESEND_INTERVAL_MS, 0));
        log.info("【are-chat 演示】手机号 {} 的注册验证码：{}", phone, code);
        return code;
    }

    /** 校验并消费验证码；失败抛业务异常 */
    public void verifyAndConsume(String phone, String code) {
        CodeEntry entry = codes.get(phone);
        if (entry == null) {
            throw new BusinessException(400, "请先获取验证码");
        }
        if (System.currentTimeMillis() > entry.expiresAt()) {
            codes.remove(phone);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (entry.attempts() >= MAX_VERIFY_ATTEMPTS) {
            codes.remove(phone);
            throw new BusinessException(429, "验证码错误次数过多，请重新获取");
        }
        if (code == null || !entry.code().equals(code.trim())) {
            codes.put(phone, new CodeEntry(entry.code(), entry.expiresAt(), entry.nextSendAt(), entry.attempts() + 1));
            throw new BusinessException(400, "验证码不正确");
        }
        codes.remove(phone);
    }

    /** 供测试/调试观察剩余有效期 */
    public long ttlSeconds() {
        return TTL_MS / 1000;
    }
}
