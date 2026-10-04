package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 情绪关怀（保留卡 `couple-comfort` 求抱抱）：发出/回应求抱抱、安慰话术卡、陪聊话题卡、情绪同步率。
 * 情绪天气预报、急救箱、和好卡、夸夸墙、生理期关怀随功能裁剪下线。
 */
@RestController
@RequestMapping("/api/couple/care")
public class CoupleCareController {

    /** F60 求抱抱请求：feeling = SAD/WRONGED/TIRED/ANXIOUS/EMO */
    public record ComfortRequest(String feeling) {
    }

    /** F60 回应求抱抱：一句安慰话（选话术卡或手写） */
    public record ComfortHandleRequest(String note) {
    }

    private final CoupleComfortService comfortService;

    public CoupleCareController(CoupleComfortService comfortService) {
        this.comfortService = comfortService;
    }

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

    /** 低落时抽 3 张话题卡，解决「不知道聊什么」。 */
    @GetMapping("/chat-topics")
    public ApiResponse<List<String>> chatTopics(HttpSession session) {
        return ApiResponse.ok(comfortService.chatTopics(Sessions.requireUser(session)));
    }

    /** 双方心情同频程度：一致占比 / 今天是否同步 / 连续同步天数。 */
    @GetMapping("/mood-sync")
    public ApiResponse<CoupleComfortService.MoodSyncVO> moodSync(HttpSession session) {
        return ApiResponse.ok(comfortService.moodSync(Sessions.requireUser(session)));
    }
}
