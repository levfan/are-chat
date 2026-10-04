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
 * 安全词与暂停复盘（保留卡 `couple-catch-safeword`）。
 * 写接口一律返回整份看板，前端不需要再拉一次。
 */
@RestController
@RequestMapping("/api/couple/catch")
public class CoupleCatchController {

    public record SafewordRequest(String word, String note) {
    }

    public record ReflectRequest(String id, String reflect) {
    }

    private final CoupleCatchService service;

    public CoupleCatchController(CoupleCatchService service) {
        this.service = service;
    }

    /** 安全词看板。 */
    @GetMapping("/board")
    public ApiResponse<CoupleCatchService.CatchVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 约定/改写自己的安全词。 */
    @PostMapping("/safeword")
    public ApiResponse<CoupleCatchService.CatchVO> safeword(@RequestBody SafewordRequest req, HttpSession session) {
        return ApiResponse.ok(service.setSafeword(Sessions.requireUser(session), req.word(), req.note()));
    }

    /** 喊一次暂停（一天一人只记一次）。 */
    @PostMapping("/safeword/use")
    public ApiResponse<CoupleCatchService.CatchVO> safewordUse(HttpSession session) {
        return ApiResponse.ok(service.useSafeword(Sessions.requireUser(session)));
    }

    /** 事后补一句复盘（只有喊停本人能补）。 */
    @PostMapping("/safeword/reflect")
    public ApiResponse<CoupleCatchService.CatchVO> safewordReflect(@RequestBody ReflectRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(service.reflectUse(Sessions.requireUser(session), req.id(), req.reflect()));
    }
}
