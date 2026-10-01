package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 身体通知系统（F310-F319）：体征互报/呼噜自报/周期共览/互助营/运动链/不适SOS/忌口红线/体检陪同/情绪药友/早睡军令状。 */
@RestController
@RequestMapping("/api/couple/body")
public class CoupleBodyController {

    private final CoupleBodyService service;

    public CoupleBodyController(CoupleBodyService service) {
        this.service = service;
    }

    /** 身体总览（近 7 天数值、近 14 天周期、当周军令状违约率、饭桌忌口撞标）。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleBodyService.BodyVO> overview(HttpSession session) {
        return ApiResponse.ok(service.body(Sessions.requireUser(session)));
    }

    /** F310 报今天的体征（超自设线首报推 TA）。 */
    public record MetricRequest(String temp, String weight, String sleepHours,
                                String tempLimit, String sleepLimit, String note) {
    }

    @PostMapping("/metric")
    public ApiResponse<CoupleBodyService.BodyVO> metric(@RequestBody MetricRequest req, HttpSession session) {
        return ApiResponse.ok(service.metric(Sessions.requireUser(session), req.temp(), req.weight(),
                req.sleepHours(), req.tempLimit(), req.sleepLimit(), req.note()));
    }

    /** F311 晨起自报呼噜档位。 */
    public record SnoreRequest(String level) {
    }

    @PostMapping("/snore")
    public ApiResponse<CoupleBodyService.BodyVO> snore(@RequestBody SnoreRequest req, HttpSession session) {
        return ApiResponse.ok(service.snore(Sessions.requireUser(session), req.level()));
    }

    /** F311 给对方补震感点评。 */
    public record ShakeRequest(String text) {
    }

    @PostMapping("/snore/shake")
    public ApiResponse<CoupleBodyService.BodyVO> shake(@RequestBody ShakeRequest req, HttpSession session) {
        return ApiResponse.ok(service.snoreShake(Sessions.requireUser(session), req.text()));
    }

    /** F312 标记自己当天的周期阶段与不适。 */
    public record CycleRequest(String day, String phase, String discomfort) {
    }

    @PostMapping("/cycle")
    public ApiResponse<CoupleBodyService.BodyVO> cycle(@RequestBody CycleRequest req, HttpSession session) {
        return ApiResponse.ok(service.cycleMark(Sessions.requireUser(session), req.day(), req.phase(), req.discomfort()));
    }

    /** F312 递照顾卡。 */
    public record CareRequest(String day, String card) {
    }

    @PostMapping("/cycle/care")
    public ApiResponse<CoupleBodyService.BodyVO> care(@RequestBody CareRequest req, HttpSession session) {
        return ApiResponse.ok(service.cycleCare(Sessions.requireUser(session), req.day(), req.card()));
    }

    /** F313 开营。 */
    public record QuitRequest(String name, Integer targetDays, String startDay) {
    }

    @PostMapping("/quit")
    public ApiResponse<CoupleBodyService.BodyVO> quit(@RequestBody QuitRequest req, HttpSession session) {
        return ApiResponse.ok(service.quitStart(Sessions.requireUser(session), req.name(), req.targetDays(), req.startDay()));
    }

    /** F313 记破戒。 */
    public record QuitIdDayRequest(String id, String day) {
    }

    @PostMapping("/quit/broke")
    public ApiResponse<CoupleBodyService.BodyVO> quitBroke(@RequestBody QuitIdDayRequest req, HttpSession session) {
        return ApiResponse.ok(service.quitBroke(Sessions.requireUser(session), req.id(), req.day()));
    }

    /** F313 陪绑方送安慰词。 */
    public record QuitCheerRequest(String id, String cheer) {
    }

    @PostMapping("/quit/cheer")
    public ApiResponse<CoupleBodyService.BodyVO> quitCheer(@RequestBody QuitCheerRequest req, HttpSession session) {
        return ApiResponse.ok(service.quitCheer(Sessions.requireUser(session), req.id(), req.cheer()));
    }

    /** F313 本人结营。 */
    public record QuitIdRequest(String id) {
    }

    @PostMapping("/quit/close")
    public ApiResponse<CoupleBodyService.BodyVO> quitClose(@RequestBody QuitIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.quitClose(Sessions.requireUser(session), req.id()));
    }

    /** F314 报今日运动计数。 */
    public record FitRequest(String kind, Integer count) {
    }

    @PostMapping("/fit")
    public ApiResponse<CoupleBodyService.BodyVO> fit(@RequestBody FitRequest req, HttpSession session) {
        return ApiResponse.ok(service.fit(Sessions.requireUser(session), req.kind(), req.count()));
    }

    /** F315 一键不舒服。 */
    public record SosRequest(String symptom, String since) {
    }

    @PostMapping("/sos")
    public ApiResponse<CoupleBodyService.BodyVO> sos(@RequestBody SosRequest req, HttpSession session) {
        return ApiResponse.ok(service.sos(Sessions.requireUser(session), req.symptom(), req.since()));
    }

    /** F315 对方接住（选一张「我能做」）。 */
    public record SosHoldRequest(String id, String comfort) {
    }

    @PostMapping("/sos/hold")
    public ApiResponse<CoupleBodyService.BodyVO> sosHold(@RequestBody SosHoldRequest req, HttpSession session) {
        return ApiResponse.ok(service.sosHold(Sessions.requireUser(session), req.id(), req.comfort()));
    }

    /** F316 登记忌口红线。 */
    public record RedlineRequest(String item, String kind, String note) {
    }

    @PostMapping("/redline")
    public ApiResponse<CoupleBodyService.BodyVO> redline(@RequestBody RedlineRequest req, HttpSession session) {
        return ApiResponse.ok(service.redlineAdd(Sessions.requireUser(session), req.item(), req.kind(), req.note()));
    }

    /** F316 划掉红线。 */
    public record RedlineIdRequest(String id) {
    }

    @PostMapping("/redline/remove")
    public ApiResponse<CoupleBodyService.BodyVO> redlineRemove(@RequestBody RedlineIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.redlineRemove(Sessions.requireUser(session), req.id()));
    }

    /** F317 约体检。 */
    public record CheckupRequest(String day, String item) {
    }

    @PostMapping("/checkup")
    public ApiResponse<CoupleBodyService.BodyVO> checkup(@RequestBody CheckupRequest req, HttpSession session) {
        return ApiResponse.ok(service.checkupPlan(Sessions.requireUser(session), req.day(), req.item()));
    }

    /** F317 虚拟陪同到场。 */
    public record CheckupIdRequest(String id) {
    }

    @PostMapping("/checkup/company")
    public ApiResponse<CoupleBodyService.BodyVO> checkupCompany(@RequestBody CheckupIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.checkupCompany(Sessions.requireUser(session), req.id()));
    }

    /** F317 检后一句话报告。 */
    public record CheckupReportRequest(String id, String report) {
    }

    @PostMapping("/checkup/report")
    public ApiResponse<CoupleBodyService.BodyVO> checkupReport(@RequestBody CheckupReportRequest req, HttpSession session) {
        return ApiResponse.ok(service.checkupReport(Sessions.requireUser(session), req.id(), req.report()));
    }

    /** F318 本周身体账（自愿，非医嘱）。 */
    public record MedRequest(String how, String note) {
    }

    @PostMapping("/med")
    public ApiResponse<CoupleBodyService.BodyVO> med(@RequestBody MedRequest req, HttpSession session) {
        return ApiResponse.ok(service.medLog(Sessions.requireUser(session), req.how(), req.note()));
    }

    /** F318 回一句陪伴话术。 */
    public record MedReplyRequest(String week, String reply) {
    }

    @PostMapping("/med/reply")
    public ApiResponse<CoupleBodyService.BodyVO> medReply(@RequestBody MedReplyRequest req, HttpSession session) {
        return ApiResponse.ok(service.medReply(Sessions.requireUser(session), req.week(), req.reply()));
    }

    /** F319 签本周熄灯线。 */
    public record OathRequest(String line) {
    }

    @PostMapping("/oath")
    public ApiResponse<CoupleBodyService.BodyVO> oath(@RequestBody OathRequest req, HttpSession session) {
        return ApiResponse.ok(service.oathSign(Sessions.requireUser(session), req.line()));
    }
}
