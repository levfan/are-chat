package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 扮演剧场（F300-F309）：身份签/互换日记/师徒日/时空电话亭/黑话大全/每日奥斯卡/家长题/双角色追剧/今日客服/冷知识颁奖礼。 */
@RestController
@RequestMapping("/api/couple/theater")
public class CoupleTheaterController {

    private final CoupleTheaterService service;

    public CoupleTheaterController(CoupleTheaterService service) {
        this.service = service;
    }

    /** 今日剧场总览（含电话亭到点接通的惰性结算，F309 颁奖礼数据在 gala 字段）。 */
    @GetMapping("/today")
    public ApiResponse<CoupleTheaterService.TheaterVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** F300 日终演技分。 */
    public record RateRequest(Integer score) {
    }

    @PostMapping("/role/rate")
    public ApiResponse<CoupleTheaterService.TheaterVO> rate(@RequestBody RateRequest req, HttpSession session) {
        return ApiResponse.ok(service.rateRole(Sessions.requireUser(session), req.score()));
    }

    /** F301 以 TA 的身份写今天这一页。 */
    public record DiaryRequest(String text) {
    }

    @PostMapping("/diary")
    public ApiResponse<CoupleTheaterService.TheaterVO> diary(@RequestBody DiaryRequest req, HttpSession session) {
        return ApiResponse.ok(service.swapDiary(Sessions.requireUser(session), req.text()));
    }

    /** F302 徒弟今日侍奉打卡。 */
    @PostMapping("/master/serve")
    public ApiResponse<CoupleTheaterService.TheaterVO> serve(HttpSession session) {
        return ApiResponse.ok(service.masterServe(Sessions.requireUser(session)));
    }

    /** F302 师父评语定级。 */
    public record MasterReviewRequest(String review, String grade) {
    }

    @PostMapping("/master/review")
    public ApiResponse<CoupleTheaterService.TheaterVO> masterReview(@RequestBody MasterReviewRequest req,
                                                                    HttpSession session) {
        return ApiResponse.ok(service.masterReview(Sessions.requireUser(session), req.review(), req.grade()));
    }

    /** F303 拨一通跨时空电话。 */
    public record BoothRequest(String kind, String text) {
    }

    @PostMapping("/booth")
    public ApiResponse<CoupleTheaterService.TheaterVO> booth(@RequestBody BoothRequest req, HttpSession session) {
        return ApiResponse.ok(service.booth(Sessions.requireUser(session), req.kind(), req.text()));
    }

    /** F304 收录黑话。 */
    public record RefRequest(String term, String meaning, String origin) {
    }

    @PostMapping("/ref")
    public ApiResponse<CoupleTheaterService.TheaterVO> addRef(@RequestBody RefRequest req, HttpSession session) {
        return ApiResponse.ok(service.addRef(Sessions.requireUser(session), req.term(), req.meaning(), req.origin()));
    }

    /** F304 抽查作答。 */
    public record RefQuizRequest(String term, String answer) {
    }

    @PostMapping("/ref/quiz")
    public ApiResponse<CoupleTheaterService.TheaterVO> quizRef(@RequestBody RefQuizRequest req, HttpSession session) {
        return ApiResponse.ok(service.quizRef(Sessions.requireUser(session), req.term(), req.answer()));
    }

    /** F304 收录人判卷。 */
    public record RefJudgeRequest(String term, Boolean right) {
    }

    @PostMapping("/ref/judge")
    public ApiResponse<CoupleTheaterService.TheaterVO> judgeRef(@RequestBody RefJudgeRequest req, HttpSession session) {
        return ApiResponse.ok(service.judgeRef(Sessions.requireUser(session), req.term(), req.right()));
    }

    /** F305 递出今日奥斯卡提名。 */
    public record AwardRequest(String evidence) {
    }

    @PostMapping("/award")
    public ApiResponse<CoupleTheaterService.TheaterVO> award(@RequestBody AwardRequest req, HttpSession session) {
        return ApiResponse.ok(service.nominate(Sessions.requireUser(session), req.evidence()));
    }

    /** F306 作答今日家长题。 */
    public record FamilyRequest(String answer) {
    }

    @PostMapping("/family")
    public ApiResponse<CoupleTheaterService.TheaterVO> family(@RequestBody FamilyRequest req, HttpSession session) {
        return ApiResponse.ok(service.familyAnswer(Sessions.requireUser(session), req.answer()));
    }

    /** F307 认领角色并追更角色日记。 */
    public record MovieRequest(String work, String roleName, String entry) {
    }

    @PostMapping("/movie")
    public ApiResponse<CoupleTheaterService.TheaterVO> movie(@RequestBody MovieRequest req, HttpSession session) {
        return ApiResponse.ok(service.movieWrite(Sessions.requireUser(session), req.work(), req.roleName(), req.entry()));
    }

    /** F307 我这一路剧终。 */
    @PostMapping("/movie/finish")
    public ApiResponse<CoupleTheaterService.TheaterVO> movieFinish(@RequestBody MovieRequest req, HttpSession session) {
        return ApiResponse.ok(service.movieFinish(Sessions.requireUser(session), req.work()));
    }

    /** F308 下服务工单。 */
    public record OrderRequest(String note) {
    }

    @PostMapping("/order")
    public ApiResponse<CoupleTheaterService.TheaterVO> order(@RequestBody OrderRequest req, HttpSession session) {
        return ApiResponse.ok(service.placeOrder(Sessions.requireUser(session), req.note()));
    }

    /** F308 客服响应。 */
    public record OrderIdRequest(String id) {
    }

    @PostMapping("/order/answer")
    public ApiResponse<CoupleTheaterService.TheaterVO> orderAnswer(@RequestBody OrderIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.answerOrder(Sessions.requireUser(session), req.id()));
    }

    /** F308 顾客评分。 */
    public record OrderScoreRequest(String id, Integer score) {
    }

    @PostMapping("/order/score")
    public ApiResponse<CoupleTheaterService.TheaterVO> orderScore(@RequestBody OrderScoreRequest req, HttpSession session) {
        return ApiResponse.ok(service.scoreOrder(Sessions.requireUser(session), req.id(), req.score()));
    }

    /** F308 客服差评申诉。 */
    public record OrderAppealRequest(String id, String appeal) {
    }

    @PostMapping("/order/appeal")
    public ApiResponse<CoupleTheaterService.TheaterVO> orderAppeal(@RequestBody OrderAppealRequest req, HttpSession session) {
        return ApiResponse.ok(service.appealOrder(Sessions.requireUser(session), req.id(), req.appeal()));
    }
}
