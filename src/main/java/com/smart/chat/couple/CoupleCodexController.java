package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 喜好 TOP10 互猜（系统裁剪后我们百科唯一保留项）。 */
@RestController
@RequestMapping("/api/couple/codex")
public class CoupleCodexController {

    private final CoupleCodexService service;

    public CoupleCodexController(CoupleCodexService service) {
        this.service = service;
    }

    /** 八个类目的榜单与猜测对照。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleCodexService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(service.overview(Sessions.requireUser(session)));
    }

    /** 类目与逗号分隔条目。 */
    public record TopRequest(String category, String items) {
    }

    /** 写自己的一份榜。 */
    @PostMapping("/top/list")
    public ApiResponse<CoupleCodexService.OverviewVO> topList(@RequestBody TopRequest req, HttpSession session) {
        return ApiResponse.ok(service.topList(Sessions.requireUser(session), req.category(), req.items()));
    }

    /** 猜 TA 的那份榜。 */
    @PostMapping("/top/guess")
    public ApiResponse<CoupleCodexService.OverviewVO> topGuess(@RequestBody TopRequest req, HttpSession session) {
        return ApiResponse.ok(service.topGuess(Sessions.requireUser(session), req.category(), req.items()));
    }
}
