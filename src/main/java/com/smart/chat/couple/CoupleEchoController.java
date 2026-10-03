package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 回音壁（系统裁剪后保留好事簿/鼓励语罐/能量补给/电量预报）。写接口一律返回整份 EchoVO。 */
@RestController
@RequestMapping("/api/couple/echo")
public class CoupleEchoController {

    private final CoupleEchoService service;

    public CoupleEchoController(CoupleEchoService service) {
        this.service = service;
    }

    /** 回音壁总览。 */
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

    /** 鼓励语正文。 */
    public record JuiceRequest(String content) {
    }

    /** 往自己罐里塞一张。 */
    @PostMapping("/juice")
    public ApiResponse<CoupleEchoService.EchoVO> juice(@RequestBody JuiceRequest req, HttpSession session) {
        return ApiResponse.ok(service.addJuice(Sessions.requireUser(session), req.content()));
    }

    /** 清掉自己罐里的一张。 */
    @PostMapping("/juice/remove")
    public ApiResponse<CoupleEchoService.EchoVO> juiceRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeJuice(Sessions.requireUser(session), req.id()));
    }

    /** 领今天的能量补给。 */
    @PostMapping("/refill")
    public ApiResponse<CoupleEchoService.EchoVO> refill(HttpSession session) {
        return ApiResponse.ok(service.refill(Sessions.requireUser(session)));
    }

    /** 电量档位与「想被怎样对待」。 */
    public record BatteryRequest(Integer level, String want) {
    }

    /** 报今天的电量。 */
    @PostMapping("/battery")
    public ApiResponse<CoupleEchoService.EchoVO> battery(@RequestBody BatteryRequest req, HttpSession session) {
        return ApiResponse.ok(service.battery(Sessions.requireUser(session), req.level(), req.want()));
    }
}
