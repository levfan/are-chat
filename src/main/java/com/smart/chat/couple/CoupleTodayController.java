package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 体验优化（F95/F96）：今日看点聚合卡 + 年度互动热力日历。
 */
@RestController
@RequestMapping("/api/couple/today")
public class CoupleTodayController {

    private final CoupleTodayService todayService;

    public CoupleTodayController(CoupleTodayService todayService) {
        this.todayService = todayService;
    }

    /** 今日看点：今天值得做的甜蜜小事清单（打卡状态聚合）。 */
    @GetMapping
    public ApiResponse<CoupleTodayService.TodayBoardVO> today(HttpSession session) {
        return ApiResponse.ok(todayService.today(Sessions.requireUser(session)));
    }

    /** 年度热力日历：指定年份（缺省当年）的互动热力。 */
    @GetMapping("/heatmap")
    public ApiResponse<CoupleTodayService.HeatmapVO> heatmap(@RequestParam(required = false) Integer year,
                                                             HttpSession session) {
        return ApiResponse.ok(todayService.heatmap(Sessions.requireUser(session), year));
    }
}
