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

import java.util.List;

/**
 * 回音壁（F350-F359，批次三十一）：好事簿/能量补给/鼓励语罐/被爱日历/感谢慢递/高光重放/
 * 夸夸回执/电量预报/写给低落的自己/回音壁年报。
 * 写接口一律返回整份 EchoVO 聚合（GET /vault 的形状），前端整体替换；GET /calendar 与 /year 除外。
 */
@RestController
@RequestMapping("/api/couple/echo")
public class CoupleEchoController {

    private final CoupleEchoService service;

    public CoupleEchoController(CoupleEchoService service) {
        this.service = service;
    }

    /** 回音壁总览（F350-F359 聚合；含 F354 慢递到日惰性结算）。 */
    @GetMapping("/vault")
    public ApiResponse<CoupleEchoService.EchoVO> vault(HttpSession session) {
        return ApiResponse.ok(service.vault(Sessions.requireUser(session)));
    }

    /** F350 记一件「TA 为我做的事」（day 缺省今天）。 */
    public record DeedRequest(String content, String day) {
    }

    @PostMapping("/deed")
    public ApiResponse<CoupleEchoService.EchoVO> deed(@RequestBody DeedRequest req, HttpSession session) {
        return ApiResponse.ok(service.addDeed(Sessions.requireUser(session), req.content(), req.day()));
    }

    /** F350 记录人本人给证据点「这条救过我」（幂等）。 */
    public record IdRequest(String id) {
    }

    @PostMapping("/deed/star")
    public ApiResponse<CoupleEchoService.EchoVO> deedStar(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.starDeed(Sessions.requireUser(session), req.id()));
    }

    /** F352 往鼓励语罐里塞一张（≤5 条）。 */
    public record JuiceRequest(String content) {
    }

    @PostMapping("/juice")
    public ApiResponse<CoupleEchoService.EchoVO> juice(@RequestBody JuiceRequest req, HttpSession session) {
        return ApiResponse.ok(service.addJuice(Sessions.requireUser(session), req.content()));
    }

    /** F352 删掉自己罐里的一张鼓励语。 */
    @PostMapping("/juice/remove")
    public ApiResponse<CoupleEchoService.EchoVO> juiceRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeJuice(Sessions.requireUser(session), req.id()));
    }

    /** F351 领今天的能量补给（每人每天一次；顺带开读自己的在途信）。 */
    @PostMapping("/refill")
    public ApiResponse<CoupleEchoService.EchoVO> refill(HttpSession session) {
        return ApiResponse.ok(service.refill(Sessions.requireUser(session)));
    }

    /** F354 寄一封感谢慢递（7 天后送达，在途每人 ≤3 封）。 */
    public record SlowRequest(String content) {
    }

    @PostMapping("/slow")
    public ApiResponse<CoupleEchoService.EchoVO> slow(@RequestBody SlowRequest req, HttpSession session) {
        return ApiResponse.ok(service.writeSlow(Sessions.requireUser(session), req.content()));
    }

    /** F355 收藏一条三行高光（moment/did/feel，每人 ≤12 条）。 */
    public record HighlightRequest(String moment, String did, String feel) {
    }

    @PostMapping("/highlight")
    public ApiResponse<CoupleEchoService.EchoVO> highlight(@RequestBody HighlightRequest req, HttpSession session) {
        return ApiResponse.ok(service.addHighlight(Sessions.requireUser(session), req.moment(), req.did(), req.feel()));
    }

    /** F355 删掉自己精选夹里的一条。 */
    @PostMapping("/highlight/remove")
    public ApiResponse<CoupleEchoService.EchoVO> highlightRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeHighlight(Sessions.requireUser(session), req.id()));
    }

    /** F356 给夸夸墙某句点「收到」（幂等，回执推给夸的人）。 */
    public record ReceiptRequest(String quoteId) {
    }

    @PostMapping("/receipt")
    public ApiResponse<CoupleEchoService.EchoVO> receipt(@RequestBody ReceiptRequest req, HttpSession session) {
        return ApiResponse.ok(service.receipt(Sessions.requireUser(session), req.quoteId()));
    }

    /** F357 报今天的电量预报（level 1-5 钳制，want ≤40 字）。 */
    public record BatteryRequest(Integer level, String want) {
    }

    @PostMapping("/battery")
    public ApiResponse<CoupleEchoService.EchoVO> battery(@RequestBody BatteryRequest req, HttpSession session) {
        return ApiResponse.ok(service.battery(Sessions.requireUser(session), req.level(), req.want()));
    }

    /** F358 写一封给低落的自己（一人同时一封在途）。 */
    public record SelfRequest(String content) {
    }

    @PostMapping("/self")
    public ApiResponse<CoupleEchoService.EchoVO> self(@RequestBody SelfRequest req, HttpSession session) {
        return ApiResponse.ok(service.writeSelf(Sessions.requireUser(session), req.content()));
    }

    /** F358 本人开读在途信置 READ（不推送给对方）。 */
    @PostMapping("/self/read")
    public ApiResponse<CoupleEchoService.EchoVO> selfRead(HttpSession session) {
        return ApiResponse.ok(service.readSelf(Sessions.requireUser(session)));
    }

    /** F353 被爱日历（按年聚合，只返回有动静的日子；year 缺省当年）。 */
    @GetMapping("/calendar")
    public ApiResponse<List<CoupleEchoService.CalendarDayVO>> calendar(
            @RequestParam(required = false) String year, HttpSession session) {
        return ApiResponse.ok(service.calendar(Sessions.requireUser(session), year));
    }

    /** F359 回音壁年报（五项真实计数 + Bank 文案 summary；year 缺省当年）。 */
    @GetMapping("/year")
    public ApiResponse<CoupleEchoService.YearlyVO> year(
            @RequestParam(required = false) String year, HttpSession session) {
        return ApiResponse.ok(service.yearReport(Sessions.requireUser(session), year));
    }
}
