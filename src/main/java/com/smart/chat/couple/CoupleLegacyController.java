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

/** 传世系统（F340-F349）：年度十问/记忆库年审/续约发布会/里程碑倒推/恋爱汇率/情侣品牌/我们的一年/传世清单/周年抽奖箱/空间等级。 */
@RestController
@RequestMapping("/api/couple/legacy")
public class CoupleLegacyController {

    private final CoupleLegacyService service;

    public CoupleLegacyController(CoupleLegacyService service) {
        this.service = service;
    }

    /** 传世系统总览（含周年抽奖提醒的读时惰性结算；goal 为 F343 目标次数，缺省 300）。 */
    @GetMapping("/vault")
    public ApiResponse<CoupleLegacyService.LegacyVO> vault(@RequestParam(required = false) Integer goal,
                                                            HttpSession session) {
        return ApiResponse.ok(service.legacy(Sessions.requireUser(session), goal));
    }

    /** F340 答年度十问的一题。 */
    public record TenRequest(String year, Integer slot, String answer) {
    }

    @PostMapping("/ten")
    public ApiResponse<CoupleLegacyService.LegacyVO> ten(@RequestBody TenRequest req, HttpSession session) {
        return ApiResponse.ok(service.tenAnswer(Sessions.requireUser(session), req.year(), req.slot(), req.answer()));
    }

    /** F341 记忆库年审。 */
    public record AuditRequest(String year, String keepThree, String deleteThree, String note) {
    }

    @PostMapping("/audit")
    public ApiResponse<CoupleLegacyService.LegacyVO> audit(@RequestBody AuditRequest req, HttpSession session) {
        return ApiResponse.ok(service.audit(Sessions.requireUser(session), req.year(), req.keepThree(),
                req.deleteThree(), req.note()));
    }

    /** F342 年度发言。 */
    public record SpeechRequest(String year, String text) {
    }

    @PostMapping("/speech")
    public ApiResponse<CoupleLegacyService.LegacyVO> speech(@RequestBody SpeechRequest req, HttpSession session) {
        return ApiResponse.ok(service.speech(Sessions.requireUser(session), req.year(), req.text()));
    }

    /** F342 对方评分卡。 */
    public record SpeechRateRequest(String year, Integer score, String note) {
    }

    @PostMapping("/speech/rate")
    public ApiResponse<CoupleLegacyService.LegacyVO> speechRate(@RequestBody SpeechRateRequest req, HttpSession session) {
        return ApiResponse.ok(service.speechRate(Sessions.requireUser(session), req.year(), req.score(), req.note()));
    }

    /** F344 报恋爱汇率。 */
    public record FxRequest(Integer kissToHug, Integer hugToWord) {
    }

    @PostMapping("/fx")
    public ApiResponse<CoupleLegacyService.LegacyVO> fx(@RequestBody FxRequest req, HttpSession session) {
        return ApiResponse.ok(service.fx(Sessions.requireUser(session), req.kissToHug(), req.hugToWord()));
    }

    /** F344 年末结算。 */
    public record FxSettleRequest(String year) {
    }

    @PostMapping("/fx/settle")
    public ApiResponse<CoupleLegacyService.LegacyVO> fxSettle(@RequestBody FxSettleRequest req, HttpSession session) {
        return ApiResponse.ok(service.fxSettle(Sessions.requireUser(session), req.year()));
    }

    /** F345 建/改情侣品牌。 */
    public record BrandRequest(String name, String slogan, String intro) {
    }

    @PostMapping("/brand")
    public ApiResponse<CoupleLegacyService.LegacyVO> brand(@RequestBody BrandRequest req, HttpSession session) {
        return ApiResponse.ok(service.brand(Sessions.requireUser(session), req.name(), req.slogan(), req.intro()));
    }

    /** F345 对方确认发布。 */
    @PostMapping("/brand/confirm")
    public ApiResponse<CoupleLegacyService.LegacyVO> brandConfirm(HttpSession session) {
        return ApiResponse.ok(service.brandConfirm(Sessions.requireUser(session)));
    }

    /** F346 一键生成年度盘点。 */
    public record ReviewRequest(String year) {
    }

    @PostMapping("/review")
    public ApiResponse<CoupleLegacyService.LegacyVO> review(@RequestBody ReviewRequest req, HttpSession session) {
        return ApiResponse.ok(service.review(Sessions.requireUser(session), req.year()));
    }

    /** F347 登记传世条目。 */
    public record ItemRequest(String item, String kind, String detail) {
    }

    @PostMapping("/item")
    public ApiResponse<CoupleLegacyService.LegacyVO> item(@RequestBody ItemRequest req, HttpSession session) {
        return ApiResponse.ok(service.itemAdd(Sessions.requireUser(session), req.item(), req.kind(), req.detail()));
    }

    /** F347 对方加签封存。 */
    public record IdRequest(String id) {
    }

    @PostMapping("/item/seal")
    public ApiResponse<CoupleLegacyService.LegacyVO> itemSeal(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.itemSeal(Sessions.requireUser(session), req.id()));
    }

    /** F348 抽今年的奖。 */
    @PostMapping("/draw")
    public ApiResponse<CoupleLegacyService.LegacyVO> draw(HttpSession session) {
        return ApiResponse.ok(service.draw(Sessions.requireUser(session)));
    }
}
