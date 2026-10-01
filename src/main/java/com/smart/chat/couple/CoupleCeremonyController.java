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

/** 小日子·仪式感（F230-F239）：建国纪念日/老黄历/过法卡/庆祝打卡/保险柜/续约/愿望券/史册/加冕/当日体感。 */
@RestController
@RequestMapping("/api/couple/ceremony")
public class CoupleCeremonyController {

    private final CoupleCeremonyService service;

    public CoupleCeremonyController(CoupleCeremonyService service) {
        this.service = service;
    }

    /** 今日仪式总览。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleCeremonyService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(service.overview(Sessions.requireUser(session)));
    }

    /** F230 新建小日子（建国纪念日）。 */
    public record FoundedRequest(String name, String startDay, Boolean repeatYear) {
    }

    @PostMapping("/founded")
    public ApiResponse<CoupleCeremonyService.OverviewVO> founded(@RequestBody FoundedRequest req, HttpSession session) {
        return ApiResponse.ok(service.addFounded(Sessions.requireUser(session),
                req.name(), req.startDay(), req.repeatYear()));
    }

    /** F230 删除小日子（连带过法卡与打卡）。 */
    public record IdRequest(String id) {
    }

    @PostMapping("/founded/remove")
    public ApiResponse<CoupleCeremonyService.OverviewVO> removeFounded(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeFounded(Sessions.requireUser(session), req.id()));
    }

    /** F232 写过法任务卡（每日子最多 3 条）。 */
    public record RitualRequest(String foundedId, String content) {
    }

    @PostMapping("/ritual")
    public ApiResponse<CoupleCeremonyService.OverviewVO> ritual(@RequestBody RitualRequest req, HttpSession session) {
        return ApiResponse.ok(service.addRitual(Sessions.requireUser(session), req.foundedId(), req.content()));
    }

    /** F232 划掉一条过法卡。 */
    @PostMapping("/ritual/remove")
    public ApiResponse<CoupleCeremonyService.OverviewVO> removeRitual(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeRitual(Sessions.requireUser(session), req.id()));
    }

    /** F233 庆祝打卡：给过法卡打勾（当日幂等）。 */
    @PostMapping("/mark")
    public ApiResponse<CoupleCeremonyService.OverviewVO> mark(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.mark(Sessions.requireUser(session), req.id()));
    }

    /** F234 交本月保费：夸 TA 一句。 */
    public record PolicyRequest(String quote) {
    }

    @PostMapping("/policy")
    public ApiResponse<CoupleCeremonyService.OverviewVO> policy(@RequestBody PolicyRequest req, HttpSession session) {
        return ApiResponse.ok(service.payPolicy(Sessions.requireUser(session), req.quote()));
    }

    /** F235 续约日签字「我还是选你」。 */
    public record RenewRequest(String line) {
    }

    @PostMapping("/renew")
    public ApiResponse<CoupleCeremonyService.OverviewVO> renew(@RequestBody RenewRequest req, HttpSession session) {
        return ApiResponse.ok(service.renew(Sessions.requireUser(session), req.line()));
    }

    /** F236 发一张愿望券。 */
    public record CouponRequest(String title) {
    }

    @PostMapping("/coupon")
    public ApiResponse<CoupleCeremonyService.OverviewVO> coupon(@RequestBody CouponRequest req, HttpSession session) {
        return ApiResponse.ok(service.issueCoupon(Sessions.requireUser(session), req.title()));
    }

    /** F236 核销一张愿望券。 */
    @PostMapping("/coupon/use")
    public ApiResponse<CoupleCeremonyService.OverviewVO> useCoupon(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.useCoupon(Sessions.requireUser(session), req.id()));
    }

    /** F239 留一句「此刻感觉」（默认今天）。 */
    public record RecapRequest(String day, String feeling) {
    }

    @PostMapping("/recap")
    public ApiResponse<CoupleCeremonyService.OverviewVO> recap(@RequestBody RecapRequest req, HttpSession session) {
        return ApiResponse.ok(service.recap(Sessions.requireUser(session), req.day(), req.feeling()));
    }

    /** F237 小日子史册：一年一页的庆祝记录与感言。 */
    @GetMapping("/chronicle")
    public ApiResponse<CoupleCeremonyService.ChronicleVO> chronicle(@RequestParam String foundedId,
                                                                    HttpSession session) {
        return ApiResponse.ok(service.chronicle(Sessions.requireUser(session), foundedId));
    }
}
