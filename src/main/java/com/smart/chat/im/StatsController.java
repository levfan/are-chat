package com.smart.chat.im;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 55 会话统计：好友数 / 收发消息 / 收藏 / 最活跃好友。 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/me")
    public ApiResponse<StatsService.StatsVO> me(HttpSession session) {
        return ApiResponse.ok(statsService.stats(Sessions.requireUser(session)));
    }
}
