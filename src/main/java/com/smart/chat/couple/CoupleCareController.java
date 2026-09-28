package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 情绪关怀：情绪天气预报 / 情绪急救箱 / 和好卡 / 夸夸墙 / 生理期关怀。
 */
@RestController
@RequestMapping("/api/couple/care")
public class CoupleCareController {

    public record ReconcileRequest(String message, Long startAt) {
    }

    public record PraiseRequest(String content) {
    }

    public record CycleRequest(String periodDay, Integer cycleDays, Integer periodDays, String note) {
    }

    private final CoupleCareService careService;

    public CoupleCareController(CoupleCareService careService) {
        this.careService = careService;
    }

    /** 今天双方的情绪天气 + 贴心提示。 */
    @GetMapping("/weather")
    public ApiResponse<CoupleCareService.WeatherVO> weather(HttpSession session) {
        return ApiResponse.ok(careService.weather(Sessions.requireUser(session)));
    }

    /** 情绪急救箱：TA 连续低落天数 + 今天怎么哄 TA。 */
    @GetMapping("/first-aid")
    public ApiResponse<CoupleCareService.FirstAidVO> firstAid(HttpSession session) {
        return ApiResponse.ok(careService.firstAid(Sessions.requireUser(session)));
    }

    // ---------- 和好卡 ----------

    /** 递一张和好卡。 */
    @PostMapping("/reconciles")
    public ApiResponse<CoupleCareService.ReconcileVO> sendReconcile(@RequestBody ReconcileRequest req,
                                                                    HttpSession session) {
        return ApiResponse.ok(careService.sendReconcile(Sessions.requireUser(session),
                req.message(), req.startAt()));
    }

    @GetMapping("/reconciles")
    public ApiResponse<List<CoupleCareService.ReconcileVO>> reconciles(HttpSession session) {
        return ApiResponse.ok(careService.listReconciles(Sessions.requireUser(session)));
    }

    /** 接受和好卡。 */
    @PostMapping("/reconciles/{id}/accept")
    public ApiResponse<CoupleCareService.ReconcileVO> acceptReconcile(@PathVariable String id,
                                                                      HttpSession session) {
        return ApiResponse.ok(careService.acceptReconcile(Sessions.requireUser(session), id));
    }

    // ---------- 夸夸墙 ----------

    /** 贴一张夸夸卡。 */
    @PostMapping("/praises")
    public ApiResponse<CoupleCareService.PraiseVO> postPraise(@RequestBody PraiseRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(careService.postPraise(Sessions.requireUser(session), req.content()));
    }

    @GetMapping("/praises")
    public ApiResponse<List<CoupleCareService.PraiseVO>> praises(HttpSession session) {
        return ApiResponse.ok(careService.listPraises(Sessions.requireUser(session)));
    }

    /** 签收夸夸卡。 */
    @PostMapping("/praises/{id}/receive")
    public ApiResponse<CoupleCareService.PraiseVO> receivePraise(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(careService.receivePraise(Sessions.requireUser(session), id));
    }

    // ---------- 生理期关怀 ----------

    /** 生理期卡片（双方记录 + 预告）。 */
    @GetMapping("/cycle")
    public ApiResponse<CoupleCareService.CycleCardVO> cycleCard(HttpSession session) {
        return ApiResponse.ok(careService.cycleCard(Sessions.requireUser(session)));
    }

    /** 记录/修改我的生理期。 */
    @PutMapping("/cycle")
    public ApiResponse<CoupleCareService.CycleCardVO> saveCycle(@RequestBody CycleRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(careService.saveCycle(Sessions.requireUser(session),
                req.periodDay(), req.cycleDays(), req.periodDays(), req.note()));
    }
}
