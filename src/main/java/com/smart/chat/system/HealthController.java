package com.smart.chat.system;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.room.ChatSessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 59 系统健康检查：免登录访问（登录页状态点使用）。
 * 返回运行时长 / 全站在线人数 / 版本号。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    public record HealthVO(String status, long uptimeSeconds, int onlineCount, String version, long serverTime) {
    }

    public static final String VERSION = "1.0.0";

    private final ChatSessionRegistry registry;
    private final long startedAt = System.currentTimeMillis();

    public HealthController(ChatSessionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ApiResponse<HealthVO> health() {
        long uptimeSeconds = (System.currentTimeMillis() - startedAt) / 1000;
        return ApiResponse.ok(new HealthVO("UP", uptimeSeconds, registry.connectionCount(), VERSION,
                System.currentTimeMillis()));
    }
}
