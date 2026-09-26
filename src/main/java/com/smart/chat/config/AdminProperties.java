package com.smart.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 管理员账号引导配置：库里没有任何 ADMIN 时，启动自动创建该账号（真实管理员，非演示账号）。
 * 生产部署务必通过环境变量 ARECHAT_ADMIN_PASSWORD 覆盖默认密码。
 */
@ConfigurationProperties(prefix = "arechat.admin")
public record AdminProperties(String username, String password) {

    public AdminProperties {
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        if (password == null || password.isBlank()) {
            password = "admin123456";
        }
    }
}
