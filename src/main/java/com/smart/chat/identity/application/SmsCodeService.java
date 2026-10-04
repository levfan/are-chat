package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.verification.SmsCode;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 手机验证码（演示环境无短信网关：验证码在响应里回显，并写入日志）。
 * 规则：6 位数字、5 分钟有效、同号 60 秒内不可重发、最多试错 5 次、用过即作废——
 * <b>规则本体在 {@link SmsCode}</b>，本类只管「条目存在哪儿、要不要删」这件存储的事。
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    private final Map<String, SmsCode> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** 生成并下发验证码，返回验证码本身（演示环境直接回显给前端） */
    public String issue(String phone) {
        long now = System.currentTimeMillis();
        SmsCode existing = codes.get(phone);
        if (existing != null) {
            DomainRules.guard(() -> existing.assertResendAllowed(now));
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(phone, SmsCode.fresh(code, now));
        log.info("【小帆船 演示】手机号 {} 的注册验证码：{}", phone, code);
        return code;
    }

    /** 校验并消费验证码；失败抛业务异常（文案与状态码由领域给出） */
    public void verifyAndConsume(String phone, String code) {
        SmsCode entry = codes.get(phone);
        if (entry == null) {
            throw new BusinessException(400, "请先获取验证码");
        }
        SmsCode.Outcome outcome = entry.verify(code, System.currentTimeMillis());
        switch (outcome.result()) {
            case CONSUMED -> codes.remove(phone);
            // 只对了一次答案，条目留着继续计数
            case WRONG_CODE -> codes.put(phone, outcome.next());
            // 过期或试错用满：条目作废，必须重新获取
            case EXPIRED, ATTEMPTS_EXCEEDED -> codes.remove(phone);
        }
        outcome.raiseIfFailed();
    }

    /** 供测试/调试观察剩余有效期 */
    public long ttlSeconds() {
        return SmsCode.ttlSeconds();
    }
}
