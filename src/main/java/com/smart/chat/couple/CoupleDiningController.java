package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 今晚饭桌（保留卡 `couple-dine-today`）：投饭票与看今天的裁决。
 */
@RestController
@RequestMapping("/api/couple/dining")
public class CoupleDiningController {

    public record TicketRequest(String dish, String reason) {
    }

    private final CoupleDiningService service;

    public CoupleDiningController(CoupleDiningService service) {
        this.service = service;
    }

    /** 今日饭桌：双方饭票 + 撞菜 + 吃什么裁决。 */
    @GetMapping("/today")
    public ApiResponse<CoupleDiningService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** 投今晚饭票（每人每天一票，重复投=改票）。 */
    @PostMapping("/ticket")
    public ApiResponse<CoupleDiningService.TodayVO> throwTicket(@RequestBody TicketRequest req, HttpSession session) {
        return ApiResponse.ok(service.throwTicket(Sessions.requireUser(session), req.dish(), req.reason()));
    }
}
