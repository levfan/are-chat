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

    /** F60 求抱抱请求：feeling = SAD/WRONGED/TIRED/ANXIOUS/EMO */
    public record ComfortRequest(String feeling) {
    }

    /** F60 回应求抱抱：一句安慰话（选话术卡或手写） */
    public record ComfortHandleRequest(String note) {
    }

    private final CoupleCareService careService;
    private final CoupleComfortService comfortService;

    public CoupleCareController(CoupleCareService careService, CoupleComfortService comfortService) {
        this.careService = careService;
        this.comfortService = comfortService;
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

    // ---------- F60 求抱抱 ----------

    /** 求抱抱看板：我今天的状态 + TA 待回应的求抱抱 + 最近记录。 */
    @GetMapping("/comfort")
    public ApiResponse<CoupleComfortService.ComfortBoardVO> comfortBoard(HttpSession session) {
        return ApiResponse.ok(comfortService.comfortBoard(Sessions.requireUser(session)));
    }

    /** 发出求抱抱（每人每天一条，重复提交视为更新感受）。 */
    @PostMapping("/comfort")
    public ApiResponse<CoupleComfortService.ComfortBoardVO> askComfort(@RequestBody ComfortRequest req,
                                                                       HttpSession session) {
        return ApiResponse.ok(comfortService.askForComfort(Sessions.requireUser(session), req.feeling()));
    }

    /** TA 的安慰话术卡：按感受随机 3 张。 */
    @GetMapping("/comfort/cards")
    public ApiResponse<List<String>> comfortCards(@org.springframework.web.bind.annotation.RequestParam String feeling,
                                                  HttpSession session) {
        Sessions.requireUser(session);
        return ApiResponse.ok(comfortService.comfortCards(feeling));
    }

    /** 回应 TA 的求抱抱（把抱抱和那句话送过去）。 */
    @PostMapping("/comfort/handle")
    public ApiResponse<CoupleComfortService.ComfortVO> handleComfort(@RequestBody ComfortHandleRequest req,
                                                                     HttpSession session) {
        return ApiResponse.ok(comfortService.handleComfort(Sessions.requireUser(session), req.note()));
    }

    // ---------- F63 陪聊话题卡 ----------

    /** 低落时抽 3 张话题卡，解决「不知道聊什么」。 */
    @GetMapping("/chat-topics")
    public ApiResponse<List<String>> chatTopics(HttpSession session) {
        return ApiResponse.ok(comfortService.chatTopics(Sessions.requireUser(session)));
    }

    // ---------- F64 情绪同步率 ----------

    /** 双方心情同频程度：一致占比 / 今天是否同步 / 连续同步天数。 */
    @GetMapping("/mood-sync")
    public ApiResponse<CoupleComfortService.MoodSyncVO> moodSync(HttpSession session) {
        return ApiResponse.ok(comfortService.moodSync(Sessions.requireUser(session)));
    }
}
