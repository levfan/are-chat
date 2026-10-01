package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 两家与朋友（F330-F339）：拜访攻略/送礼池/他观问卷/官宣日/文案代写/接待手册/称呼册/社会信用/群聊记者/代 TA 赔礼。 */
@RestController
@RequestMapping("/api/couple/world")
public class CoupleWorldController {

    private final CoupleWorldService service;

    public CoupleWorldController(CoupleWorldService service) {
        this.service = service;
    }

    /** 两家与朋友总览（含保证到期自动解除的惰性结算）。 */
    @GetMapping("/world")
    public ApiResponse<CoupleWorldService.WorldVO> world(HttpSession session) {
        return ApiResponse.ok(service.world(Sessions.requireUser(session)));
    }

    /** F330 写拜访攻略。 */
    public record VisitRequest(String day, String hostSide, String preps) {
    }

    @PostMapping("/visit")
    public ApiResponse<CoupleWorldService.WorldVO> visit(@RequestBody VisitRequest req, HttpSession session) {
        return ApiResponse.ok(service.visitPlan(Sessions.requireUser(session), req.day(), req.hostSide(), req.preps()));
    }

    /** F330 确认攻略。 */
    public record IdRequest(String id) {
    }

    @PostMapping("/visit/confirm")
    public ApiResponse<CoupleWorldService.WorldVO> visitConfirm(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.visitConfirm(Sessions.requireUser(session), req.id()));
    }

    /** F330 交战报。 */
    public record VisitReportRequest(String id, String report) {
    }

    @PostMapping("/visit/report")
    public ApiResponse<CoupleWorldService.WorldVO> visitReport(@RequestBody VisitReportRequest req, HttpSession session) {
        return ApiResponse.ok(service.visitReport(Sessions.requireUser(session), req.id(), req.report()));
    }

    /** F331 收送礼灵感。 */
    public record GiftRequest(String person, String idea, String budget, String avoid) {
    }

    @PostMapping("/gift")
    public ApiResponse<CoupleWorldService.WorldVO> gift(@RequestBody GiftRequest req, HttpSession session) {
        return ApiResponse.ok(service.giftAdd(Sessions.requireUser(session), req.person(), req.idea(),
                req.budget(), req.avoid()));
    }

    @PostMapping("/gift/take")
    public ApiResponse<CoupleWorldService.WorldVO> giftTake(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.giftTake(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/gift/bought")
    public ApiResponse<CoupleWorldService.WorldVO> giftBought(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.giftBought(Sessions.requireUser(session), req.id()));
    }

    /** F332 回填他观一题。 */
    public record ViewRequest(Integer slot, String askedTo, String answer) {
    }

    @PostMapping("/friendView")
    public ApiResponse<CoupleWorldService.WorldVO> friendView(@RequestBody ViewRequest req, HttpSession session) {
        return ApiResponse.ok(service.friendViewFill(Sessions.requireUser(session), req.slot(), req.askedTo(), req.answer()));
    }

    /** F333 发本月官宣卡。 */
    public record DeclareRequest(String text) {
    }

    @PostMapping("/declare")
    public ApiResponse<CoupleWorldService.WorldVO> declare(@RequestBody DeclareRequest req, HttpSession session) {
        return ApiResponse.ok(service.declare(Sessions.requireUser(session), req.text()));
    }

    /** F334 交候选文案。 */
    public record CaptionRequest(Integer slot, String text) {
    }

    @PostMapping("/caption")
    public ApiResponse<CoupleWorldService.WorldVO> caption(@RequestBody CaptionRequest req, HttpSession session) {
        return ApiResponse.ok(service.captionSubmit(Sessions.requireUser(session), req.slot(), req.text()));
    }

    /** F334 互评选稿。 */
    @PostMapping("/caption/pick")
    public ApiResponse<CoupleWorldService.WorldVO> captionPick(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.captionPick(Sessions.requireUser(session), req.id()));
    }

    /** F335 存接待手册。 */
    public record CityRequest(String city, String arriveDay, String itinerary, String transport, String packList) {
    }

    @PostMapping("/city")
    public ApiResponse<CoupleWorldService.WorldVO> city(@RequestBody CityRequest req, HttpSession session) {
        return ApiResponse.ok(service.citySave(Sessions.requireUser(session), req.city(), req.arriveDay(),
                req.itinerary(), req.transport(), req.packList()));
    }

    /** F336 出称谓题。 */
    public record RelativeRequest(String term, String question, String answer) {
    }

    @PostMapping("/relative")
    public ApiResponse<CoupleWorldService.WorldVO> relative(@RequestBody RelativeRequest req, HttpSession session) {
        return ApiResponse.ok(service.relativeAdd(Sessions.requireUser(session), req.term(), req.question(), req.answer()));
    }

    /** F336 作答称谓题。 */
    public record RelativeTryRequest(String id, String answer) {
    }

    @PostMapping("/relative/try")
    public ApiResponse<CoupleWorldService.WorldVO> relativeTry(@RequestBody RelativeTryRequest req, HttpSession session) {
        return ApiResponse.ok(service.relativeTry(Sessions.requireUser(session), req.id(), req.answer()));
    }

    /** F337 立保证。 */
    public record VowRequest(String content, String dueDay) {
    }

    @PostMapping("/vow")
    public ApiResponse<CoupleWorldService.WorldVO> vow(@RequestBody VowRequest req, HttpSession session) {
        return ApiResponse.ok(service.vowAdd(Sessions.requireUser(session), req.content(), req.dueDay()));
    }

    /** F337 见证。 */
    @PostMapping("/vow/witness")
    public ApiResponse<CoupleWorldService.WorldVO> vowWitness(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.vowWitness(Sessions.requireUser(session), req.id()));
    }

    /** F337 塌房记录。 */
    public record VowBreakRequest(String id, String note) {
    }

    @PostMapping("/vow/break")
    public ApiResponse<CoupleWorldService.WorldVO> vowBreak(@RequestBody VowBreakRequest req, HttpSession session) {
        return ApiResponse.ok(service.vowBreak(Sessions.requireUser(session), req.id(), req.note()));
    }

    /** F338 交今日群聊素材。 */
    public record GroupRequest(String line) {
    }

    @PostMapping("/group")
    public ApiResponse<CoupleWorldService.WorldVO> group(@RequestBody GroupRequest req, HttpSession session) {
        return ApiResponse.ok(service.groupLine(Sessions.requireUser(session), req.line()));
    }

    /** F338 笑了对方那条。 */
    @PostMapping("/group/laugh")
    public ApiResponse<CoupleWorldService.WorldVO> groupLaugh(HttpSession session) {
        return ApiResponse.ok(service.groupLaugh(Sessions.requireUser(session)));
    }

    /** F339 写赔礼信。 */
    public record ApologyRequest(String toPerson, String reason, String draft) {
    }

    @PostMapping("/apology")
    public ApiResponse<CoupleWorldService.WorldVO> apology(@RequestBody ApologyRequest req, HttpSession session) {
        return ApiResponse.ok(service.apologyWrite(Sessions.requireUser(session), req.toPerson(), req.reason(), req.draft()));
    }

    /** F339 TA 审阅。 */
    public record ApologyReviewRequest(String id, Boolean pass, String note) {
    }

    @PostMapping("/apology/review")
    public ApiResponse<CoupleWorldService.WorldVO> apologyReview(@RequestBody ApologyReviewRequest req, HttpSession session) {
        return ApiResponse.ok(service.apologyReview(Sessions.requireUser(session), req.id(), req.pass(), req.note()));
    }

    /** F339 打回后重写。 */
    public record ApologyRewriteRequest(String id, String draft) {
    }

    @PostMapping("/apology/rewrite")
    public ApiResponse<CoupleWorldService.WorldVO> apologyRewrite(@RequestBody ApologyRewriteRequest req, HttpSession session) {
        return ApiResponse.ok(service.apologyRewrite(Sessions.requireUser(session), req.id(), req.draft()));
    }
}
