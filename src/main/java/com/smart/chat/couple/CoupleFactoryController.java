package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 家务轮盘（系统裁剪后二人制造厂唯一保留项）。 */
@RestController
@RequestMapping("/api/couple/factory")
public class CoupleFactoryController {

    private final CoupleFactoryService service;

    public CoupleFactoryController(CoupleFactoryService service) {
        this.service = service;
    }

    /** 本周车间总览（轮盘分工 + 前几周欠账）。 */
    @GetMapping("/board")
    public ApiResponse<CoupleFactoryService.BoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 逗号分隔事项。 */
    public record SpinRequest(String items) {
    }

    /** 一转定分工，一周一转。 */
    @PostMapping("/spin")
    public ApiResponse<CoupleFactoryService.BoardVO> spin(@RequestBody SpinRequest req, HttpSession session) {
        return ApiResponse.ok(service.spin(Sessions.requireUser(session), req.items()));
    }

    /** 任务 id。 */
    public record IdRequest(String id) {
    }

    /** 对方认账这一格。 */
    @PostMapping("/spin/confirm")
    public ApiResponse<CoupleFactoryService.BoardVO> spinConfirm(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.confirmSpin(Sessions.requireUser(session), req.id()));
    }

    /** 干的人自己打勾。 */
    @PostMapping("/spin/done")
    public ApiResponse<CoupleFactoryService.BoardVO> spinDone(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.doneSpin(Sessions.requireUser(session), req.id()));
    }
}
