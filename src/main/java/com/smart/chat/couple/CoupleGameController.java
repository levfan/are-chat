package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 恋爱游戏化：今日心动加成 / 互动热力图 / 心情曲线 / 恋爱红绿灯。
 */
@RestController
@RequestMapping("/api/couple/game")
public class CoupleGameController {

    private final CoupleGameService gameService;

    public CoupleGameController(CoupleGameService gameService) {
        this.gameService = gameService;
    }

    /** 今日心动加成：今天的互动按项计分，24 点清零。 */
    @GetMapping("/boost")
    public ApiResponse<CoupleGameService.IntimacyBoostVO> boost(HttpSession session) {
        return ApiResponse.ok(gameService.todayBoost(Sessions.requireUser(session)));
    }

    /** 互动热力图：最近 12 周每天的互动强度。 */
    @GetMapping("/heatmap")
    public ApiResponse<CoupleGameService.HeatmapVO> heatmap(HttpSession session) {
        return ApiResponse.ok(gameService.heatmap(Sessions.requireUser(session)));
    }

    /** 心情曲线：最近 30 天双方心情走势。 */
    @GetMapping("/mood-curve")
    public ApiResponse<CoupleGameService.MoodCurveVO> moodCurve(HttpSession session) {
        return ApiResponse.ok(gameService.moodCurve(Sessions.requireUser(session)));
    }

    /** 恋爱红绿灯：多久没互动了 + 怎么办。 */
    @GetMapping("/traffic-light")
    public ApiResponse<CoupleGameService.TrafficLightVO> trafficLight(HttpSession session) {
        return ApiResponse.ok(gameService.trafficLight(Sessions.requireUser(session)));
    }
}
