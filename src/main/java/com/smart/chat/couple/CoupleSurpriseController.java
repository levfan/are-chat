package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 惊喜与期待（F50-F53/F57/F58）：爱情刮刮乐、恋爱盲盒、心动闹钟、思念速递、告白重现、藏宝图任务。
 */
@RestController
@RequestMapping("/api/couple/surprise")
public class CoupleSurpriseController {

    public record BoxCreateRequest(String kind, String content, String openDay) {
    }

    public record AlarmCreateRequest(String message, Long fireAt) {
    }

    public record TreasureCreateRequest(String taskText, String prizeText) {
    }

    public record ConfessionCreateRequest(String content, String confessDay) {
    }

    private final CoupleSurpriseService surpriseService;

    public CoupleSurpriseController(CoupleSurpriseService surpriseService) {
        this.surpriseService = surpriseService;
    }

    // ---------- F50 爱情刮刮乐 ----------

    /** 我的刮刮乐（自动补发本周的卡）。 */
    @GetMapping("/scratches")
    public ApiResponse<List<CoupleSurpriseService.ScratchVO>> scratches(HttpSession session) {
        return ApiResponse.ok(surpriseService.myScratches(Sessions.requireUser(session)));
    }

    /** 刮开我的券。 */
    @PostMapping("/scratches/{id}/scratch")
    public ApiResponse<CoupleSurpriseService.ScratchVO> scratch(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(surpriseService.scratch(Sessions.requireUser(session), id));
    }

    /** 送券人核销（承诺闭环）。 */
    @PostMapping("/scratches/{id}/redeem")
    public ApiResponse<CoupleSurpriseService.ScratchVO> redeemScratch(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(surpriseService.redeemScratch(Sessions.requireUser(session), id));
    }

    // ---------- F51 恋爱盲盒 ----------

    @GetMapping("/boxes")
    public ApiResponse<List<CoupleSurpriseService.BoxVO>> boxes(HttpSession session) {
        return ApiResponse.ok(surpriseService.boxes(Sessions.requireUser(session)));
    }

    /** 装一个盲盒（最早明天开箱）。 */
    @PostMapping("/boxes")
    public ApiResponse<CoupleSurpriseService.BoxVO> createBox(@RequestBody BoxCreateRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(surpriseService.createBox(Sessions.requireUser(session),
                req.kind(), req.content(), req.openDay()));
    }

    /** 开盲盒（到开箱日才能拆）。 */
    @PostMapping("/boxes/{id}/open")
    public ApiResponse<CoupleSurpriseService.BoxVO> openBox(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(surpriseService.openBox(Sessions.requireUser(session), id));
    }

    // ---------- F52 心动闹钟 ----------

    @GetMapping("/alarms")
    public ApiResponse<List<CoupleSurpriseService.AlarmVO>> alarms(HttpSession session) {
        return ApiResponse.ok(surpriseService.alarms(Sessions.requireUser(session)));
    }

    /** 设一个心动闹钟（未来 24 小时内）。 */
    @PostMapping("/alarms")
    public ApiResponse<CoupleSurpriseService.AlarmVO> createAlarm(@RequestBody AlarmCreateRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(surpriseService.createAlarm(Sessions.requireUser(session),
                req.message(), req.fireAt()));
    }

    @DeleteMapping("/alarms/{id}")
    public ApiResponse<Void> cancelAlarm(@PathVariable String id, HttpSession session) {
        surpriseService.cancelAlarm(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- F53 思念速递 ----------

    @GetMapping("/misses")
    public ApiResponse<CoupleSurpriseService.MissBoardVO> missBoard(HttpSession session) {
        return ApiResponse.ok(surpriseService.missBoard(Sessions.requireUser(session)));
    }

    /** 寄出一份思念（5~30 分钟随机送达）。 */
    @PostMapping("/misses")
    public ApiResponse<CoupleSurpriseService.MissBoardVO> sendMiss(HttpSession session) {
        return ApiResponse.ok(surpriseService.sendMiss(Sessions.requireUser(session)));
    }

    // ---------- F58 藏宝图任务 ----------

    @GetMapping("/treasures")
    public ApiResponse<List<CoupleSurpriseService.TreasureVO>> treasures(HttpSession session) {
        return ApiResponse.ok(surpriseService.treasures(Sessions.requireUser(session)));
    }

    /** 埋一个宝藏（任务 + 藏好的奖品）。 */
    @PostMapping("/treasures")
    public ApiResponse<CoupleSurpriseService.TreasureVO> createTreasure(@RequestBody TreasureCreateRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(surpriseService.createTreasure(Sessions.requireUser(session),
                req.taskText(), req.prizeText()));
    }

    /** 完成任务挖宝（宝藏揭晓）。 */
    @PostMapping("/treasures/{id}/done")
    public ApiResponse<CoupleSurpriseService.TreasureVO> completeTreasure(@PathVariable String id,
                                                                          HttpSession session) {
        return ApiResponse.ok(surpriseService.completeTreasure(Sessions.requireUser(session), id));
    }

    // ---------- F57 告白重现 ----------

    @GetMapping("/confessions")
    public ApiResponse<List<CoupleSurpriseService.ConfessionVO>> confessions(HttpSession session) {
        return ApiResponse.ok(surpriseService.confessions(Sessions.requireUser(session)));
    }

    /** 收藏一段告白（每年今天自动重播）。 */
    @PostMapping("/confessions")
    public ApiResponse<CoupleSurpriseService.ConfessionVO> createConfession(@RequestBody ConfessionCreateRequest req,
                                                                            HttpSession session) {
        return ApiResponse.ok(surpriseService.createConfession(Sessions.requireUser(session),
                req.content(), req.confessDay()));
    }

    @DeleteMapping("/confessions/{id}")
    public ApiResponse<Void> deleteConfession(@PathVariable String id, HttpSession session) {
        surpriseService.deleteConfession(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }
}
