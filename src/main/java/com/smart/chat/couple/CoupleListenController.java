package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 误会倒带（系统裁剪后倾听与发声唯一保留项）。 */
@RestController
@RequestMapping("/api/couple/listen")
public class CoupleListenController {

    private final CoupleListenService service;

    public CoupleListenController(CoupleListenService service) {
        this.service = service;
    }

    /** 最近 30 天的倒带对照总览。 */
    @GetMapping("/today")
    public ApiResponse<CoupleListenService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** 主题 + 两侧的「我当时以为/我猜你其实想」。 */
    public record MisRequest(String topic, String mine, String theirs) {
    }

    /** 就一次争执写自己那一份。 */
    @PostMapping("/misrewind")
    public ApiResponse<CoupleListenService.TodayVO> misrewind(@RequestBody MisRequest req, HttpSession session) {
        return ApiResponse.ok(service.misrewind(Sessions.requireUser(session), req.topic(), req.mine(), req.theirs()));
    }
}
