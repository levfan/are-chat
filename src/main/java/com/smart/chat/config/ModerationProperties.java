package com.smart.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 86 敏感词过滤配置：
 * - enabled=false 时完全关闭
 * - mode=censor：命中词替换为等长 *（默认）；mode=block：直接拦截发送
 * - sensitive-words：运维在 application.yml / 环境变量维护的词库（用 | 分隔的yml数组）
 */
@ConfigurationProperties(prefix = "arechat.moderation")
public record ModerationProperties(boolean enabled, String mode, List<String> sensitiveWords) {

    public static final String MODE_CENSOR = "censor";
    public static final String MODE_BLOCK = "block";

    public ModerationProperties {
        if (mode == null || mode.isBlank()) {
            mode = MODE_CENSOR;
        }
        if (sensitiveWords == null) {
            sensitiveWords = List.of();
        }
    }

    public boolean blockMode() {
        return MODE_BLOCK.equalsIgnoreCase(mode);
    }
}
