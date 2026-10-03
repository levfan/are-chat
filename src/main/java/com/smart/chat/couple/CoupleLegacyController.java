package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 周年抽奖箱（系统裁剪后传世系统唯一保留项）。 */
@RestController
@RequestMapping("/api/couple/legacy")
public class CoupleLegacyController {

    private final CoupleLegacyService service;

    public CoupleLegacyController(CoupleLegacyService service) {
        this.service = service;
    }

    /** 抽奖箱总览（含周年提醒的读时惰性结算）。 */
    @GetMapping("/vault")
    public ApiResponse<CoupleLegacyService.LegacyVO> vault(HttpSession session) {
        return ApiResponse.ok(service.legacy(Sessions.requireUser(session)));
    }

    /** 抽今年的奖，每人一年一次。 */
    @PostMapping("/draw")
    public ApiResponse<CoupleLegacyService.LegacyVO> draw(HttpSession session) {
        return ApiResponse.ok(service.draw(Sessions.requireUser(session)));
    }
}
