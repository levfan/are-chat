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

/**
 * 异地恋·时空同步（F110-F119，批次七）：隔空牵手 / 双城时刻卡（前端渲染） /
 * 想念计量所 / 见面能量瓶 / 我们的作息表 / 下次见面信 / 云约会清单 / 平安卡 / 见面日记 / 异地恋报告。
 */
@RestController
@RequestMapping("/api/couple/distance")
public class CoupleDistanceController {

    public record RoutineRequest(String wakeTime, String workStart, String workEnd, String sleepTime) {
    }

    public record LetterRequest(String content) {
    }

    public record CloudDateRequest(String item) {
    }

    public record CloudDoneRequest(String note) {
    }

    public record SafetyRequest(String kind, String note) {
    }

    public record ReunionRequest(String meetDay, String note) {
    }

    private final CoupleDistanceService distanceService;

    public CoupleDistanceController(CoupleDistanceService distanceService) {
        this.distanceService = distanceService;
    }

    // ---------- F110 隔空牵手 ----------

    @GetMapping("/handhold")
    public ApiResponse<CoupleDistanceService.HandholdVO> handhold(HttpSession session) {
        return ApiResponse.ok(distanceService.handhold(Sessions.requireUser(session)));
    }

    /** 点亮今天的手（双方都点亮即牵手成功）。 */
    @PostMapping("/handhold")
    public ApiResponse<CoupleDistanceService.HandholdVO> holdHand(HttpSession session) {
        return ApiResponse.ok(distanceService.holdHand(Sessions.requireUser(session)));
    }

    // ---------- F112 想念计量所 ----------

    @GetMapping("/miss")
    public ApiResponse<CoupleDistanceService.MissVO> miss(HttpSession session) {
        return ApiResponse.ok(distanceService.miss(Sessions.requireUser(session)));
    }

    /** 点亮「今天想你了」（同天互想=双向奔赴）。 */
    @PostMapping("/miss")
    public ApiResponse<CoupleDistanceService.MissVO> lightMiss(HttpSession session) {
        return ApiResponse.ok(distanceService.lightMiss(Sessions.requireUser(session)));
    }

    // ---------- F114 我们的作息表 ----------

    @GetMapping("/routine")
    public ApiResponse<CoupleDistanceService.RoutineVO> routine(HttpSession session) {
        return ApiResponse.ok(distanceService.routine(Sessions.requireUser(session)));
    }

    /** 保存我的作息（起床/上班/下班/睡觉）。 */
    @PostMapping("/routine")
    public ApiResponse<CoupleDistanceService.RoutineVO> saveRoutine(@RequestBody RoutineRequest req,
                                                                    HttpSession session) {
        return ApiResponse.ok(distanceService.saveRoutine(Sessions.requireUser(session),
                req.wakeTime(), req.workStart(), req.workEnd(), req.sleepTime()));
    }

    // ---------- F115 下次见面信 ----------

    @GetMapping("/letters")
    public ApiResponse<List<CoupleDistanceService.LetterVO>> letters(HttpSession session) {
        return ApiResponse.ok(distanceService.reunionLetters(Sessions.requireUser(session)));
    }

    /** 写一封见面信。 */
    @PostMapping("/letters")
    public ApiResponse<List<CoupleDistanceService.LetterVO>> writeLetter(@RequestBody LetterRequest req,
                                                                         HttpSession session) {
        return ApiResponse.ok(distanceService.writeLetter(Sessions.requireUser(session), req.content()));
    }

    /** 拆信（见面打卡后）。 */
    @PostMapping("/letters/{id}/open")
    public ApiResponse<List<CoupleDistanceService.LetterVO>> openLetter(@PathVariable String id,
                                                                        HttpSession session) {
        return ApiResponse.ok(distanceService.openLetter(Sessions.requireUser(session), id));
    }

    // ---------- F116 云约会清单 ----------

    @GetMapping("/cloud-dates")
    public ApiResponse<List<CoupleDistanceService.CloudDateVO>> cloudDates(HttpSession session) {
        return ApiResponse.ok(distanceService.cloudDates(Sessions.requireUser(session)));
    }

    /** 添加云约会（item 为空则从灵感库抽一条）。 */
    @PostMapping("/cloud-dates")
    public ApiResponse<List<CoupleDistanceService.CloudDateVO>> addCloudDate(@RequestBody(required = false) CloudDateRequest req,
                                                                             HttpSession session) {
        return ApiResponse.ok(distanceService.addCloudDate(Sessions.requireUser(session),
                req == null ? null : req.item()));
    }

    /** 完成云约会打卡。 */
    @PostMapping("/cloud-dates/{id}/done")
    public ApiResponse<List<CoupleDistanceService.CloudDateVO>> doneCloudDate(@PathVariable String id,
                                                                              @RequestBody(required = false) CloudDoneRequest req,
                                                                              HttpSession session) {
        return ApiResponse.ok(distanceService.doneCloudDate(Sessions.requireUser(session), id,
                req == null ? null : req.note()));
    }

    // ---------- F117 平安卡 ----------

    @GetMapping("/safeties")
    public ApiResponse<List<CoupleDistanceService.SafetyVO>> safeties(HttpSession session) {
        return ApiResponse.ok(distanceService.safeties(Sessions.requireUser(session)));
    }

    /** 一键报平安（出发/到家）。 */
    @PostMapping("/safeties")
    public ApiResponse<List<CoupleDistanceService.SafetyVO>> pingSafety(@RequestBody SafetyRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(distanceService.pingSafety(Sessions.requireUser(session),
                req.kind(), req.note()));
    }

    // ---------- F118 见面日记 ----------

    @GetMapping("/reunions")
    public ApiResponse<List<CoupleDistanceService.ReunionLogVO>> reunions(HttpSession session) {
        return ApiResponse.ok(distanceService.reunions(Sessions.requireUser(session)));
    }

    /** 记一笔见面（同天一条，可补记）。 */
    @PostMapping("/reunions")
    public ApiResponse<List<CoupleDistanceService.ReunionLogVO>> logReunion(@RequestBody ReunionRequest req,
                                                                            HttpSession session) {
        return ApiResponse.ok(distanceService.logReunion(Sessions.requireUser(session),
                req.meetDay(), req.note()));
    }

    // ---------- F113 见面能量瓶 ----------

    /** 离上次见面越久能量越满，见面归零。 */
    @GetMapping("/energy")
    public ApiResponse<CoupleDistanceService.EnergyVO> energy(HttpSession session) {
        return ApiResponse.ok(distanceService.energy(Sessions.requireUser(session)));
    }

    // ---------- F119 异地恋报告 ----------

    @GetMapping("/report")
    public ApiResponse<CoupleDistanceService.DistanceReportVO> report(HttpSession session) {
        return ApiResponse.ok(distanceService.report(Sessions.requireUser(session)));
    }
}
