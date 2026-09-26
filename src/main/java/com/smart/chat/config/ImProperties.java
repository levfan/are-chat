package com.smart.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IM 行为配置：87 发送频率限制（防刷屏）。
 * send-limit-per-minute：单个用户每分钟最多发送的私信条数，超过返回 429 并提示冷却。
 */
@ConfigurationProperties(prefix = "arechat.im")
public record ImProperties(int sendLimitPerMinute) {

    public ImProperties {
        if (sendLimitPerMinute <= 0) {
            sendLimitPerMinute = 30;
        }
    }
}
