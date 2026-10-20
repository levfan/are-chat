package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleCalendarService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 亲密日历（卡根 `couple-calendar`）：按月给一份「经期覆盖日 + 亲密标记」的只读结果。
 * 只有读口，写仍然走经期卡与亲密卡各自的端点——日历负责看一眼，不负责记账。
 */
@RestController
@RequestMapping("/api/couple/calendar")
public class CoupleCalendarController {

    private final CoupleCalendarService service;

    public CoupleCalendarController(CoupleCalendarService service) {
        this.service = service;
    }

    /** 整月日历：month=yyyy-MM。 */
    @GetMapping("/month")
    public ApiResponse<CoupleCalendarService.CoupleCalendarVO> month(
            @RequestParam(required = false) String month, HttpSession session) {
        return ApiResponse.ok(service.month(Sessions.requireUser(session), month));
    }
}
