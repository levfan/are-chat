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
 * 深度陪伴·生活分享（F140-F149，批次十）：今日主题曲 / 梦境手账 / 美食地图 /
 * TA 使用手册 / 情绪 SOS / 每日三问 / 夸夸生成器 / 接头暗号 / 自定义成就 / 恋爱仪表盘。
 */
@RestController
@RequestMapping("/api/couple/daily-life")
public class CoupleDailyLifeController {

    public record DreamRequest(String content) {
    }

    public record FoodRequest(String shop, String dish) {
    }

    public record FoodCheckinRequest(Integer rating, String comment) {
    }

    public record FactRequest(String kind, String content) {
    }

    public record SosRequest(String message) {
    }

    public record ThreeRequest(String joy, String touched, String wantToSay) {
    }

    public record BadgeRequest(String title, String condition) {
    }

    private final CoupleDailyLifeService dailyLifeService;

    public CoupleDailyLifeController(CoupleDailyLifeService dailyLifeService) {
        this.dailyLifeService = dailyLifeService;
    }

    // ---------- F140 今日主题曲 ----------

    @GetMapping("/theme-song")
    public ApiResponse<CoupleDailyLifeService.ThemeSongVO> themeSong(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.themeSong(Sessions.requireUser(session)));
    }

    // ---------- F141 梦境手账 ----------

    @GetMapping("/dreams")
    public ApiResponse<List<CoupleDailyLifeService.DreamVO>> dreams(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.dreams(Sessions.requireUser(session)));
    }

    /** 写下一个梦。 */
    @PostMapping("/dreams")
    public ApiResponse<List<CoupleDailyLifeService.DreamVO>> writeDream(@RequestBody DreamRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(dailyLifeService.writeDream(Sessions.requireUser(session), req.content()));
    }

    // ---------- F142 美食地图 ----------

    @GetMapping("/foods")
    public ApiResponse<List<CoupleDailyLifeService.FoodNoteVO>> foods(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.foods(Sessions.requireUser(session)));
    }

    /** 添加想吃的店。 */
    @PostMapping("/foods")
    public ApiResponse<List<CoupleDailyLifeService.FoodNoteVO>> addFood(@RequestBody FoodRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(dailyLifeService.addFood(Sessions.requireUser(session), req.shop(), req.dish()));
    }

    /** 打卡：吃过啦。 */
    @PostMapping("/foods/{id}/checkin")
    public ApiResponse<List<CoupleDailyLifeService.FoodNoteVO>> checkinFood(@PathVariable String id,
                                                                            @RequestBody FoodCheckinRequest req,
                                                                            HttpSession session) {
        return ApiResponse.ok(dailyLifeService.checkinFood(Sessions.requireUser(session), id,
                req.rating(), req.comment()));
    }

    // ---------- F143 TA 使用手册 ----------

    @GetMapping("/facts")
    public ApiResponse<List<CoupleDailyLifeService.FactVO>> facts(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.facts(Sessions.requireUser(session)));
    }

    /** 补一页说明书。 */
    @PostMapping("/facts")
    public ApiResponse<List<CoupleDailyLifeService.FactVO>> addFact(@RequestBody FactRequest req,
                                                                    HttpSession session) {
        return ApiResponse.ok(dailyLifeService.addFact(Sessions.requireUser(session), req.kind(), req.content()));
    }

    // ---------- F144 情绪 SOS ----------

    @GetMapping("/soses")
    public ApiResponse<List<CoupleDailyLifeService.SosVO>> soses(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.soses(Sessions.requireUser(session)));
    }

    /** 一键求抱抱。 */
    @PostMapping("/soses")
    public ApiResponse<List<CoupleDailyLifeService.SosVO>> pingSos(@RequestBody SosRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(dailyLifeService.pingSos(Sessions.requireUser(session), req.message()));
    }

    /** 抱住：接住对方的 SOS。 */
    @PostMapping("/soses/{id}/hold")
    public ApiResponse<List<CoupleDailyLifeService.SosVO>> holdSos(@PathVariable String id,
                                                                   HttpSession session) {
        return ApiResponse.ok(dailyLifeService.holdSos(Sessions.requireUser(session), id));
    }

    // ---------- F145 每日三问 ----------

    @GetMapping("/three")
    public ApiResponse<CoupleDailyLifeService.ThreeVO> dailyThree(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.dailyThree(Sessions.requireUser(session)));
    }

    /** 提交/修改今日三问。 */
    @PostMapping("/three")
    public ApiResponse<CoupleDailyLifeService.ThreeVO> saveDailyThree(@RequestBody ThreeRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(dailyLifeService.saveDailyThree(Sessions.requireUser(session),
                req.joy(), req.touched(), req.wantToSay()));
    }

    // ---------- F146 夸夸生成器 + F147 接头暗号 ----------

    @GetMapping("/praise")
    public ApiResponse<CoupleDailyLifeService.PraiseVO> praise(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.praise(Sessions.requireUser(session)));
    }

    // ---------- F148 自定义成就 ----------

    @GetMapping("/badges")
    public ApiResponse<List<CoupleDailyLifeService.BadgeVO>> badges(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.badges(Sessions.requireUser(session)));
    }

    /** 立一个成就。 */
    @PostMapping("/badges")
    public ApiResponse<List<CoupleDailyLifeService.BadgeVO>> addBadge(@RequestBody BadgeRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(dailyLifeService.addBadge(Sessions.requireUser(session), req.title(), req.condition()));
    }

    /** 达成颁发双人证书。 */
    @PostMapping("/badges/{id}/issue")
    public ApiResponse<List<CoupleDailyLifeService.BadgeVO>> issueBadge(@PathVariable String id,
                                                                        HttpSession session) {
        return ApiResponse.ok(dailyLifeService.issueBadge(Sessions.requireUser(session), id));
    }

    // ---------- F149 恋爱仪表盘 ----------

    @GetMapping("/dashboard")
    public ApiResponse<CoupleDailyLifeService.DashboardVO> dashboard(HttpSession session) {
        return ApiResponse.ok(dailyLifeService.dashboard(Sessions.requireUser(session)));
    }
}
