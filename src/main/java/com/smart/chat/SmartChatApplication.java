package com.smart.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * are-chat 后端服务入口。
 * 前身是 2019 年的 Spring 4 + JSP 单体 war，现改造为前后端分离的 REST + WebSocket 服务。
 * 持久层为 MyBatis-Plus：SqlSessionFactory 与 @MapperScan 在 MybatisPlusConfig 手工装配
 * （Boot 4 下 starter 自动配置不生效；放在普通 @Configuration 里可避免污染 @WebMvcTest 切片）。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SmartChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartChatApplication.class, args);
    }
}
