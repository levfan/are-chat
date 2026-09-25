package com.smart.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置：去重后的文件按内容哈希落盘，baseDir 可通过 arechat.storage.base-dir 配置。
 */
@ConfigurationProperties(prefix = "arechat.storage")
public record FileStorageProperties(String baseDir) {

    public FileStorageProperties {
        if (baseDir == null || baseDir.isBlank()) {
            baseDir = "./uploads";
        }
    }
}
