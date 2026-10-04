package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleEchoService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 好事簿（保留卡 `couple-echo-deed`）：记一件「TA 为我做的事」，给证据加星。
 * 写接口一律返回整份看板。
 */
@RestController
@RequestMapping("/api/couple/echo")
public class CoupleEchoController {

    private final CoupleEchoService service;

    public CoupleEchoController(CoupleEchoService service) {
        this.service = service;
    }

    /** 好事簿看板。 */
    @GetMapping("/vault")
    public ApiResponse<CoupleEchoService.EchoVO> vault(HttpSession session) {
        return ApiResponse.ok(service.vault(Sessions.requireUser(session)));
    }

    /** 好事正文与发生日。 */
    public record DeedRequest(String content, String day) {
    }

    /** 记一件「TA 为我做的事」。 */
    @PostMapping("/deed")
    public ApiResponse<CoupleEchoService.EchoVO> deed(@RequestBody DeedRequest req, HttpSession session) {
        return ApiResponse.ok(service.addDeed(Sessions.requireUser(session), req.content(), req.day()));
    }

    /** 记录行 id。 */
    public record IdRequest(String id) {
    }

    /** 给这条证据加星。 */
    @PostMapping("/deed/star")
    public ApiResponse<CoupleEchoService.EchoVO> deedStar(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.starDeed(Sessions.requireUser(session), req.id()));
    }
}
