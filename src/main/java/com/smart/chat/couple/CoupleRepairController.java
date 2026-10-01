package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 修复车间（F320-F329）：冷冻解冻/道歉质检/重来卡/信任重建/和好了倒计时/冲突年报/底线卡/我错了榜/修复礼盒/和平纪念碑。 */
@RestController
@RequestMapping("/api/couple/repair")
public class CoupleRepairController {

    private final CoupleRepairService service;

    public CoupleRepairController(CoupleRepairService service) {
        this.service = service;
    }

    /** 修复车间总览（含倒计时到点自动递台阶卡的惰性结算，F325 年报在 report 字段）。 */
    @GetMapping("/workshop")
    public ApiResponse<CoupleRepairService.RepairVO> workshop(HttpSession session) {
        return ApiResponse.ok(service.workshop(Sessions.requireUser(session)));
    }

    /** F320 挂冷冻。 */
    public record FreezeRequest(Integer hours, String reason) {
    }

    @PostMapping("/freeze")
    public ApiResponse<CoupleRepairService.RepairVO> freeze(@RequestBody FreezeRequest req, HttpSession session) {
        return ApiResponse.ok(service.freeze(Sessions.requireUser(session), req.hours(), req.reason()));
    }

    /** F320 答解冻三问。 */
    public record FreezeAskRequest(String id, Integer slot, String answer) {
    }

    @PostMapping("/freeze/ask")
    public ApiResponse<CoupleRepairService.RepairVO> freezeAsk(@RequestBody FreezeAskRequest req, HttpSession session) {
        return ApiResponse.ok(service.freezeAsk(Sessions.requireUser(session), req.id(), req.slot(), req.answer()));
    }

    /** F320 签解冻。 */
    public record FreezeIdRequest(String id) {
    }

    @PostMapping("/freeze/sign")
    public ApiResponse<CoupleRepairService.RepairVO> freezeSign(@RequestBody FreezeIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.freezeSign(Sessions.requireUser(session), req.id()));
    }

    /** F321 交道歉信。 */
    public record SorryRequest(String letter, String points) {
    }

    @PostMapping("/sorry")
    public ApiResponse<CoupleRepairService.RepairVO> sorry(@RequestBody SorryRequest req, HttpSession session) {
        return ApiResponse.ok(service.sorryWrite(Sessions.requireUser(session), req.letter(), req.points()));
    }

    /** F321 打回后重写。 */
    public record SorryRewriteRequest(String id, String letter, String points) {
    }

    @PostMapping("/sorry/rewrite")
    public ApiResponse<CoupleRepairService.RepairVO> sorryRewrite(@RequestBody SorryRewriteRequest req, HttpSession session) {
        return ApiResponse.ok(service.sorryRewrite(Sessions.requireUser(session), req.id(), req.letter(), req.points()));
    }

    /** F321 对方验货。 */
    public record SorryVerifyRequest(String id, Boolean pass, String verdict) {
    }

    @PostMapping("/sorry/verify")
    public ApiResponse<CoupleRepairService.RepairVO> sorryVerify(@RequestBody SorryVerifyRequest req, HttpSession session) {
        return ApiResponse.ok(service.sorryVerify(Sessions.requireUser(session), req.id(), req.pass(), req.verdict()));
    }

    /** F322 领重来卡。 */
    public record RedoRequest(String scene) {
    }

    @PostMapping("/redo")
    public ApiResponse<CoupleRepairService.RepairVO> redo(@RequestBody RedoRequest req, HttpSession session) {
        return ApiResponse.ok(service.redoApply(Sessions.requireUser(session), req.scene()));
    }

    /** F322 重放完成。 */
    public record RedoPlayRequest(String replayNote) {
    }

    @PostMapping("/redo/play")
    public ApiResponse<CoupleRepairService.RepairVO> redoPlay(@RequestBody RedoPlayRequest req, HttpSession session) {
        return ApiResponse.ok(service.redoPlay(Sessions.requireUser(session), req.replayNote()));
    }

    /** F322 重放满意度。 */
    public record RedoRateRequest(Integer satisfaction) {
    }

    @PostMapping("/redo/rate")
    public ApiResponse<CoupleRepairService.RepairVO> redoRate(@RequestBody RedoRateRequest req, HttpSession session) {
        return ApiResponse.ok(service.redoRate(Sessions.requireUser(session), req.satisfaction()));
    }

    /** F323 开重建计划。 */
    public record RebuildRequest(String name, String cause, Integer targetDays, String tasks) {
    }

    @PostMapping("/rebuild")
    public ApiResponse<CoupleRepairService.RepairVO> rebuild(@RequestBody RebuildRequest req, HttpSession session) {
        return ApiResponse.ok(service.rebuildStart(Sessions.requireUser(session), req.name(), req.cause(),
                req.targetDays(), req.tasks()));
    }

    /** F323 每日双签。 */
    public record PlanSignRequest(String id, String day) {
    }

    @PostMapping("/rebuild/sign")
    public ApiResponse<CoupleRepairService.RepairVO> rebuildSign(@RequestBody PlanSignRequest req, HttpSession session) {
        return ApiResponse.ok(service.rebuildSign(Sessions.requireUser(session), req.id(), req.day()));
    }

    /** F323 周复盘。 */
    public record PlanReviewRequest(String id, String review) {
    }

    @PostMapping("/rebuild/review")
    public ApiResponse<CoupleRepairService.RepairVO> rebuildReview(@RequestBody PlanReviewRequest req, HttpSession session) {
        return ApiResponse.ok(service.rebuildReview(Sessions.requireUser(session), req.id(), req.review()));
    }

    /** F323 中止计划。 */
    @PostMapping("/rebuild/giveup")
    public ApiResponse<CoupleRepairService.RepairVO> rebuildGiveup(@RequestBody PlanSignRequest req, HttpSession session) {
        return ApiResponse.ok(service.rebuildGiveup(Sessions.requireUser(session), req.id()));
    }

    /** F324 开冷战倒计时。 */
    public record MakeupRequest(Integer minutes) {
    }

    @PostMapping("/makeup")
    public ApiResponse<CoupleRepairService.RepairVO> makeup(@RequestBody MakeupRequest req, HttpSession session) {
        return ApiResponse.ok(service.makeupStart(Sessions.requireUser(session), req.minutes()));
    }

    /** F324 对方暂停/继续。 */
    public record MakeupPauseRequest(String id, Boolean pause) {
    }

    @PostMapping("/makeup/pause")
    public ApiResponse<CoupleRepairService.RepairVO> makeupPause(@RequestBody MakeupPauseRequest req, HttpSession session) {
        return ApiResponse.ok(service.makeupPause(Sessions.requireUser(session), req.id(), req.pause()));
    }

    /** F324 提前递台阶。 */
    @PostMapping("/makeup/offer")
    public ApiResponse<CoupleRepairService.RepairVO> makeupOffer(@RequestBody FreezeIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.makeupOffer(Sessions.requireUser(session), req.id()));
    }

    /** F324 宣布和好（掉礼盒）。 */
    @PostMapping("/makeup/end")
    public ApiResponse<CoupleRepairService.RepairVO> makeupEnd(@RequestBody FreezeIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.makeupEnd(Sessions.requireUser(session), req.id()));
    }

    /** F326 声明底线。 */
    public record BottomRequest(Integer slot, String text, String sinceDay) {
    }

    @PostMapping("/bottom")
    public ApiResponse<CoupleRepairService.RepairVO> bottom(@RequestBody BottomRequest req, HttpSession session) {
        return ApiResponse.ok(service.bottomSet(Sessions.requireUser(session), req.slot(), req.text(), req.sinceDay()));
    }

    /** F326 踩线补红线记录。 */
    public record BreachRequest(String id, String note) {
    }

    @PostMapping("/bottom/breach")
    public ApiResponse<CoupleRepairService.RepairVO> breach(@RequestBody BreachRequest req, HttpSession session) {
        return ApiResponse.ok(service.bottomBreach(Sessions.requireUser(session), req.id(), req.note()));
    }

    /** F327 认错。 */
    public record AdmitRequest(String detail) {
    }

    @PostMapping("/admit")
    public ApiResponse<CoupleRepairService.RepairVO> admit(@RequestBody AdmitRequest req, HttpSession session) {
        return ApiResponse.ok(service.admit(Sessions.requireUser(session), req.detail()));
    }

    /** F327 标最感人认错。 */
    @PostMapping("/admit/touch")
    public ApiResponse<CoupleRepairService.RepairVO> admitTouch(@RequestBody FreezeIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.admitTouch(Sessions.requireUser(session), req.id()));
    }

    /** F328 完成补偿任务。 */
    @PostMapping("/box/done")
    public ApiResponse<CoupleRepairService.RepairVO> boxDone(@RequestBody FreezeIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.boxDone(Sessions.requireUser(session), req.id()));
    }

    /** F329 立和平纪念碑。 */
    public record PeaceRequest(String line, String note) {
    }

    @PostMapping("/peace")
    public ApiResponse<CoupleRepairService.RepairVO> peace(@RequestBody PeaceRequest req, HttpSession session) {
        return ApiResponse.ok(service.peaceSet(Sessions.requireUser(session), req.line(), req.note()));
    }
}
