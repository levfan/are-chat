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

/**
 * 注意力保护区（F360-F369，批次三十二）：专注打卡/专属时段/攒一句话/饭桌不低头/对视十秒/
 * 不插电半小时/走神温柔哨/专注周报/数字排毒半天/注意力年报。
 * 写接口一律返回整份 TodayVO 聚合（GET /today 的形状），前端整体替换；GET /weekly 与 /year 除外。
 */
@RestController
@RequestMapping("/api/couple/focus")
public class CoupleFocusController {

    private final CoupleFocusService service;

    public CoupleFocusController(CoupleFocusService service) {
        this.service = service;
    }

    /** 今日注意力总览（顺带签收 to_user=me 的未读留言）。 */
    @GetMapping("/today")
    public ApiResponse<CoupleFocusService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** F363 饭桌手机倒扣打卡（双方各点各的，双点=同桌成功）。 */
    @PostMapping("/meal")
    public ApiResponse<CoupleFocusService.TodayVO> meal(HttpSession session) {
        return ApiResponse.ok(service.mealTick(Sessions.requireUser(session)));
    }

    /** F364 对视十秒打卡（双点点亮）。 */
    @PostMapping("/gaze")
    public ApiResponse<CoupleFocusService.TodayVO> gaze(HttpSession session) {
        return ApiResponse.ok(service.gazeTick(Sessions.requireUser(session)));
    }

    /** F366 递一张「回来啦」卡（每人每天 ≤2 张，note ≤40 字）。 */
    public record NudgeRequest(String note) {
    }

    @PostMapping("/nudge")
    public ApiResponse<CoupleFocusService.TodayVO> nudge(@RequestBody NudgeRequest req, HttpSession session) {
        return ApiResponse.ok(service.nudge(Sessions.requireUser(session), req.note()));
    }

}
