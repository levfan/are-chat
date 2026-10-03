package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聆听者（F380-F389，批次三十四）：暗中心愿本/雷区探测器/安全词/敏感日历/说到哪了/
 * 真话翻译机/聆听方式协议/话题许愿池/今日一句话/聆听者年报。
 * 写接口一律返回整份 CatchVO 聚合（GET /board 的形状），GET /year 除外。
 */
@RestController
@RequestMapping("/api/couple/catch")
public class CoupleCatchController {

    private final CoupleCatchService service;

    public CoupleCatchController(CoupleCatchService service) {
        this.service = service;
    }

    /** 聆听者总览（F380-F389 聚合，含当年年报一行）。 */
    @GetMapping("/board")
    public ApiResponse<CoupleCatchService.CatchVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** F380 悄悄记一条 TA 随口说的心愿（不推给对方）。 */
    public record WishRequest(String content, String sourceDay, String scene) {
    }

    @PostMapping("/wish")
    public ApiResponse<CoupleCatchService.CatchVO> wish(@RequestBody WishRequest req, HttpSession session) {
        return ApiResponse.ok(service.addWish(Sessions.requireUser(session), req.content(), req.sourceDay(),
                req.scene()));
    }

    public record IdRequest(String id) {
    }

    /** F380 兑现登记（勾完立刻揭晓并推给心愿主人）。 */
    @PostMapping("/wish/fulfill")
    public ApiResponse<CoupleCatchService.CatchVO> wishFulfill(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.fulfillWish(Sessions.requireUser(session), req.id()));
    }

    /** F381 挂一颗雷（话题 ≤30、雷点与安全说法各 ≤60，每人 ≤6 颗）。 */
    public record MineRequest(String topic, String trip, String safeWay) {
    }

    @PostMapping("/mine")
    public ApiResponse<CoupleCatchService.CatchVO> mine(@RequestBody MineRequest req, HttpSession session) {
        return ApiResponse.ok(service.addMine(Sessions.requireUser(session), req.topic(), req.trip(),
                req.safeWay()));
    }

    /** F381 对方盖「已知晓」（自己挂的不能自己盖，重复盖幂等）。 */
    @PostMapping("/mine/ack")
    public ApiResponse<CoupleCatchService.CatchVO> mineAck(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.ackMine(Sessions.requireUser(session), req.id()));
    }

    /** F381 记一次成功避雷（要先盖过知晓）。 */
    @PostMapping("/mine/avoid")
    public ApiResponse<CoupleCatchService.CatchVO> mineAvoid(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.avoidMine(Sessions.requireUser(session), req.id()));
    }

    /** F382 约定/改写自己的安全词（word ≤20、note ≤60）。 */
    public record SafewordRequest(String word, String note) {
    }

    @PostMapping("/safeword")
    public ApiResponse<CoupleCatchService.CatchVO> safeword(@RequestBody SafewordRequest req, HttpSession session) {
        return ApiResponse.ok(service.setSafeword(Sessions.requireUser(session), req.word(), req.note()));
    }

    /** F382 喊了一次暂停（一天一人只记一次）。 */
    @PostMapping("/safeword/use")
    public ApiResponse<CoupleCatchService.CatchVO> safewordUse(HttpSession session) {
        return ApiResponse.ok(service.useSafeword(Sessions.requireUser(session)));
    }

    /** F382 事后补一句复盘（只有喊停本人能补）。 */
    public record ReflectRequest(String id, String reflect) {
    }

    @PostMapping("/safeword/reflect")
    public ApiResponse<CoupleCatchService.CatchVO> safewordReflect(@RequestBody ReflectRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(service.reflectUse(Sessions.requireUser(session), req.id(), req.reflect()));
    }

    /** F388 留今天想对 TA 说的一句（≤40 字）。 */
    public record DailyRequest(String content) {
    }

    @PostMapping("/daily")
    public ApiResponse<CoupleCatchService.CatchVO> daily(@RequestBody DailyRequest req, HttpSession session) {
        return ApiResponse.ok(service.daily(Sessions.requireUser(session), req.content()));
    }
}
