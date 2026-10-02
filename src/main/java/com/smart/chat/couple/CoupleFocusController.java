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

    /** F360 报今晚放下的分钟数（0-180 钳制，note ≤40 字）。 */
    public record NightRequest(Integer minutes, String note) {
    }

    @PostMapping("/night")
    public ApiResponse<CoupleFocusService.TodayVO> night(@RequestBody NightRequest req, HttpSession session) {
        return ApiResponse.ok(service.night(Sessions.requireUser(session), req.minutes(), req.note()));
    }

    /** F361 预约本周专属时段（title ≤60、hours 1-6 钳制、day 必须落在本周）。 */
    public record SlotRequest(String title, Integer hours, String day) {
    }

    @PostMapping("/slot/propose")
    public ApiResponse<CoupleFocusService.TodayVO> slotPropose(@RequestBody SlotRequest req, HttpSession session) {
        return ApiResponse.ok(service.proposeSlot(Sessions.requireUser(session), req.title(), req.hours(), req.day()));
    }

    /** F361 对方点头确认本周时段（自己提议的不能自己确认）。 */
    @PostMapping("/slot/confirm")
    public ApiResponse<CoupleFocusService.TodayVO> slotConfirm(HttpSession session) {
        return ApiResponse.ok(service.confirmSlot(Sessions.requireUser(session)));
    }

    /** F362 把一句话攒进队列（≤80 字，在途每人 ≤5）。 */
    public record QueueRequest(String content) {
    }

    @PostMapping("/queue")
    public ApiResponse<CoupleFocusService.TodayVO> queue(@RequestBody QueueRequest req, HttpSession session) {
        return ApiResponse.ok(service.queueAdd(Sessions.requireUser(session), req.content()));
    }

    /** F362 一键收全部并回执已读。 */
    @PostMapping("/queue/read")
    public ApiResponse<CoupleFocusService.TodayVO> queueRead(HttpSession session) {
        return ApiResponse.ok(service.queueRead(Sessions.requireUser(session)));
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

    /** F365 不插电半小时打卡（双点成功，周连击读时算）。 */
    @PostMapping("/unplug")
    public ApiResponse<CoupleFocusService.TodayVO> unplug(HttpSession session) {
        return ApiResponse.ok(service.unplugTick(Sessions.requireUser(session)));
    }

    /** F366 递一张「回来啦」卡（每人每天 ≤2 张，note ≤40 字）。 */
    public record NudgeRequest(String note) {
    }

    @PostMapping("/nudge")
    public ApiResponse<CoupleFocusService.TodayVO> nudge(@RequestBody NudgeRequest req, HttpSession session) {
        return ApiResponse.ok(service.nudge(Sessions.requireUser(session), req.note()));
    }

    /** F368 发起/应战半日无手机挑战（kind 只能 AM 或 PM）。 */
    public record DetoxRequest(String kind) {
    }

    @PostMapping("/detox")
    public ApiResponse<CoupleFocusService.TodayVO> detox(@RequestBody DetoxRequest req, HttpSession session) {
        return ApiResponse.ok(service.detox(Sessions.requireUser(session), req.kind()));
    }

    /** F367 专注周报（周一锚聚合 + Bank 文案）。 */
    @GetMapping("/weekly")
    public ApiResponse<CoupleFocusService.WeeklyVO> weekly(HttpSession session) {
        return ApiResponse.ok(service.weekly(Sessions.requireUser(session)));
    }

    /** F369 注意力年报（year 缺省当年）。 */
    @GetMapping("/year")
    public ApiResponse<CoupleFocusService.YearlyVO> year(
            @RequestParam(required = false) String year, HttpSession session) {
        return ApiResponse.ok(service.yearReport(Sessions.requireUser(session), year));
    }
}
