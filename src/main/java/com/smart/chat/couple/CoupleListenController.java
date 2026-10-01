package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 倾听与发声（F260-F269）：想被听时段/替我说/误会倒带/卡壳一问/换位信/早想说/三行打卡/语气翻译/休战旗/称呼日。 */
@RestController
@RequestMapping("/api/couple/listen")
public class CoupleListenController {

    private final CoupleListenService service;

    public CoupleListenController(CoupleListenService service) {
        this.service = service;
    }

    /** 今日倾听台总览（含早想说惰性放行、休战到期结算）。 */
    @GetMapping("/today")
    public ApiResponse<CoupleListenService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** F260 申请「只听我说」时段。 */
    public record SlotRequest(String topic) {
    }

    @PostMapping("/slot")
    public ApiResponse<CoupleListenService.TodayVO> slot(@RequestBody SlotRequest req, HttpSession session) {
        return ApiResponse.ok(service.requestSlot(Sessions.requireUser(session), req.topic()));
    }

    public record IdRequest(String id) {
    }

    @PostMapping("/slot/confirm")
    public ApiResponse<CoupleListenService.TodayVO> slotConfirm(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.confirmSlot(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/slot/done")
    public ApiResponse<CoupleListenService.TodayVO> slotDone(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.doneSlot(Sessions.requireUser(session), req.id()));
    }

    /** F260 互评被听感。 */
    public record RateRequest(String id, Integer score, String note) {
    }

    @PostMapping("/slot/rate")
    public ApiResponse<CoupleListenService.TodayVO> slotRate(@RequestBody RateRequest req, HttpSession session) {
        return ApiResponse.ok(service.rateSlot(Sessions.requireUser(session), req.id(), req.score(), req.note()));
    }

    /** F261 替 TA 写一句（在途草稿可覆盖）。 */
    public record ProxyRequest(String content) {
    }

    @PostMapping("/proxy")
    public ApiResponse<CoupleListenService.TodayVO> proxy(@RequestBody ProxyRequest req, HttpSession session) {
        return ApiResponse.ok(service.proxy(Sessions.requireUser(session), req.content()));
    }

    /** F261 被代笔人定稿。 */
    public record AdoptRequest(String id, String finalText) {
    }

    @PostMapping("/proxy/adopt")
    public ApiResponse<CoupleListenService.TodayVO> proxyAdopt(@RequestBody AdoptRequest req, HttpSession session) {
        return ApiResponse.ok(service.adoptProxy(Sessions.requireUser(session), req.id(), req.finalText()));
    }

    /** F262 误会倒带。 */
    public record MisRequest(String topic, String mine, String theirs) {
    }

    @PostMapping("/misrewind")
    public ApiResponse<CoupleListenService.TodayVO> misrewind(@RequestBody MisRequest req, HttpSession session) {
        return ApiResponse.ok(service.misrewind(Sessions.requireUser(session), req.topic(), req.mine(), req.theirs()));
    }

    /** F263 本周卡壳一问。 */
    public record QuestionRequest(String question) {
    }

    @PostMapping("/stuck")
    public ApiResponse<CoupleListenService.TodayVO> stuck(@RequestBody QuestionRequest req, HttpSession session) {
        return ApiResponse.ok(service.stuck(Sessions.requireUser(session), req.question()));
    }

    public record AnswerRequest(String id, String answer) {
    }

    @PostMapping("/stuck/answer")
    public ApiResponse<CoupleListenService.TodayVO> stuckAnswer(@RequestBody AnswerRequest req, HttpSession session) {
        return ApiResponse.ok(service.answerStuck(Sessions.requireUser(session), req.id(), req.answer()));
    }

    /** F264 写换位信。 */
    public record LetterRequest(String content, String openDay) {
    }

    @PostMapping("/letter")
    public ApiResponse<CoupleListenService.TodayVO> letter(@RequestBody LetterRequest req, HttpSession session) {
        return ApiResponse.ok(service.letter(Sessions.requireUser(session), req.content(), req.openDay()));
    }

    /** F264 拆 TA 写给我的信。 */
    @PostMapping("/letter/open")
    public ApiResponse<CoupleListenService.TodayVO> letterOpen(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.openLetter(Sessions.requireUser(session), req.id()));
    }

    /** F265 封存一句早想说。 */
    public record HoldRequest(String content) {
    }

    @PostMapping("/hold")
    public ApiResponse<CoupleListenService.TodayVO> hold(@RequestBody HoldRequest req, HttpSession session) {
        return ApiResponse.ok(service.hold(Sessions.requireUser(session), req.content()));
    }

    /** F266 今日三行打卡。 */
    public record ThreeRequest(String morning, String thanks, String praise) {
    }

    @PostMapping("/three")
    public ApiResponse<CoupleListenService.TodayVO> three(@RequestBody ThreeRequest req, HttpSession session) {
        return ApiResponse.ok(service.three(Sessions.requireUser(session), req.morning(), req.thanks(), req.praise()));
    }

    /** F267 自报今日语气。 */
    public record ToneRequest(String tone, String note) {
    }

    @PostMapping("/tone")
    public ApiResponse<CoupleListenService.TodayVO> tone(@RequestBody ToneRequest req, HttpSession session) {
        return ApiResponse.ok(service.tone(Sessions.requireUser(session), req.tone(), req.note()));
    }

    /** F268 举休战旗。 */
    public record TruceRequest(Integer minutes) {
    }

    @PostMapping("/truce")
    public ApiResponse<CoupleListenService.TodayVO> truce(@RequestBody(required = false) TruceRequest req,
                                                          HttpSession session) {
        return ApiResponse.ok(service.truce(Sessions.requireUser(session), req == null ? null : req.minutes()));
    }

    /** F268 到点表态：继续/算了。 */
    public record DecideRequest(boolean goOn) {
    }

    @PostMapping("/truce/decide")
    public ApiResponse<CoupleListenService.TodayVO> truceDecide(@RequestBody DecideRequest req, HttpSession session) {
        return ApiResponse.ok(service.decideTruce(Sessions.requireUser(session), req.goOn()));
    }

    /** F269 今日称呼「用过了」。 */
    @PostMapping("/name/use")
    public ApiResponse<CoupleListenService.TodayVO> nameUse(HttpSession session) {
        return ApiResponse.ok(service.useName(Sessions.requireUser(session)));
    }
}
