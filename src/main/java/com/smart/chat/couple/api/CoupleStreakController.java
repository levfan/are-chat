package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleStreakService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连续互动打卡（新卡 `couple-streak`）：看板与补签，写接口返回整份看板。
 */
@RestController
@RequestMapping("/api/couple/streak")
public class CoupleStreakController {

    /** 补签的日子（yyyy-MM-dd）。 */
    public record MakeupRequest(String day) {
    }

    private final CoupleStreakService service;

    public CoupleStreakController(CoupleStreakService service) {
        this.service = service;
    }

    /** 打卡看板：连续天数、七档解锁进度、补签额度与近 21 天打卡条。 */
    @GetMapping("/board")
    public ApiResponse<CoupleStreakService.StreakBoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 补一次签（花积分，只能补最近 7 天里的缺口）。 */
    @PostMapping("/makeup")
    public ApiResponse<CoupleStreakService.StreakBoardVO> makeup(@RequestBody MakeupRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(service.makeup(Sessions.requireUser(session), req.day()));
    }
}
