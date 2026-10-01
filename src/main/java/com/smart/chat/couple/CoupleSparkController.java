package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 默契亲密系（F170-F179）：爱语测评/对照、心动闪光、「如果」问答、动作暗语、同频共振、仪表盘、心动日历、排行、周报。 */
@RestController
@RequestMapping("/api/couple/spark")
public class CoupleSparkController {

    private final CoupleSparkService service;

    public CoupleSparkController(CoupleSparkService service) {
        this.service = service;
    }

    /** F170 爱语测评卷（静态）。 */
    @GetMapping("/love-lang/quiz")
    public ApiResponse<List<CoupleSparkBank.QuizQuestion>> quiz() {
        return ApiResponse.ok(service.quiz());
    }

    /** F170 提交爱语答卷。 */
    public record LoveLangRequest(List<String> answers) {
    }

    @PostMapping("/love-lang")
    public ApiResponse<CoupleSparkService.LoveLangVO> submitLoveLang(@RequestBody LoveLangRequest req, HttpSession session) {
        return ApiResponse.ok(service.submitLoveLang(Sessions.requireUser(session), req.answers()));
    }

    /** F170 我的爱语结果。 */
    @GetMapping("/love-lang/mine")
    public ApiResponse<CoupleSparkService.LoveLangVO> myLoveLang(HttpSession session) {
        return ApiResponse.ok(service.myLoveLang(Sessions.requireUser(session)));
    }

    /** F171 爱语对照卡。 */
    @GetMapping("/love-lang/pair")
    public ApiResponse<CoupleSparkService.LoveLangPairVO> loveLangPair(HttpSession session) {
        return ApiResponse.ok(service.loveLangPair(Sessions.requireUser(session)));
    }

    /** F172 心动闪光列表。 */
    @GetMapping("/flashes")
    public ApiResponse<List<CoupleSparkService.FlashVO>> flashes(HttpSession session) {
        return ApiResponse.ok(service.flashes(Sessions.requireUser(session)));
    }

    /** F172 速记心动。 */
    public record FlashRequest(String moment) {
    }

    @PostMapping("/flashes")
    public ApiResponse<List<CoupleSparkService.FlashVO>> addFlash(@RequestBody FlashRequest req, HttpSession session) {
        return ApiResponse.ok(service.addFlash(Sessions.requireUser(session), req.moment()));
    }

    /** F173 今日「如果」。 */
    @GetMapping("/what-if")
    public ApiResponse<CoupleSparkService.WhatIfVO> whatIf(HttpSession session) {
        return ApiResponse.ok(service.whatIf(Sessions.requireUser(session)));
    }

    /** F173 回答「如果」。 */
    public record WhatIfRequest(String answer) {
    }

    @PostMapping("/what-if")
    public ApiResponse<CoupleSparkService.WhatIfVO> answerWhatIf(@RequestBody WhatIfRequest req, HttpSession session) {
        return ApiResponse.ok(service.answerWhatIf(Sessions.requireUser(session), req.answer()));
    }

    /** F174 动作暗语列表。 */
    @GetMapping("/signals")
    public ApiResponse<List<CoupleSparkService.SignalVO>> signals(HttpSession session) {
        return ApiResponse.ok(service.signals(Sessions.requireUser(session)));
    }

    /** F174 约定动作暗语。 */
    public record SignalRequest(String signal, String meaning) {
    }

    @PostMapping("/signals")
    public ApiResponse<List<CoupleSparkService.SignalVO>> addSignal(@RequestBody SignalRequest req, HttpSession session) {
        return ApiResponse.ok(service.addSignal(Sessions.requireUser(session), req.signal(), req.meaning()));
    }

    /** F175 按键（同频共振）。 */
    @PostMapping("/tap")
    public ApiResponse<CoupleSparkService.TapResultVO> tap(HttpSession session) {
        return ApiResponse.ok(service.tap(Sessions.requireUser(session)));
    }

    /** F175 今日按键状态。 */
    @GetMapping("/tap/today")
    public ApiResponse<CoupleSparkService.TapResultVO> todayTap(HttpSession session) {
        return ApiResponse.ok(service.todayTap(Sessions.requireUser(session)));
    }

    /** F176 默契仪表盘。 */
    @GetMapping("/dashboard")
    public ApiResponse<CoupleSparkService.SparkDashboardVO> dashboard(HttpSession session) {
        return ApiResponse.ok(service.dashboard(Sessions.requireUser(session)));
    }

    /** F177 心动日历。 */
    @GetMapping("/heart-days")
    public ApiResponse<List<CoupleSparkService.HeartDayVO>> heartDays(HttpSession session) {
        return ApiResponse.ok(service.heartDays(Sessions.requireUser(session)));
    }

    /** F177 盖心动邮戳。 */
    public record HeartDayRequest(Integer level) {
    }

    @PostMapping("/heart-days")
    public ApiResponse<List<CoupleSparkService.HeartDayVO>> markHeartDay(@RequestBody HeartDayRequest req, HttpSession session) {
        return ApiResponse.ok(service.markHeartDay(Sessions.requireUser(session), req.level()));
    }

    /** F178 同频排行榜。 */
    @GetMapping("/sync-rank")
    public ApiResponse<List<CoupleSparkService.SyncRankVO>> syncRank(HttpSession session) {
        return ApiResponse.ok(service.syncRank(Sessions.requireUser(session)));
    }

    /** F179 默契周报。 */
    @GetMapping("/weekly")
    public ApiResponse<CoupleSparkService.SparkWeeklyVO> weekly(HttpSession session) {
        return ApiResponse.ok(service.weekly(Sessions.requireUser(session)));
    }
}
