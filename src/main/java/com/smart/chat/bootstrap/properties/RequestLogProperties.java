package com.smart.chat.bootstrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口访问日志配置：/api/** 每个请求打两行（进入=入参，完成=状态+耗时+响应体）。
 * enabled=false 时一行都不打（生产想关掉用这个，不要改代码）。
 * maxBodyChars 单行正文最多记录多少字符，超出打截断标记并注明总长；
 * 它同时是响应体在内存里的缓冲上限，所以不支持「不限」，避免大响应整份进堆。
 */
@ConfigurationProperties(prefix = "arechat.request-log")
public record RequestLogProperties(Boolean enabled, int maxBodyChars) {

    public RequestLogProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (maxBodyChars <= 0) {
            maxBodyChars = 4000;
        }
    }
}
