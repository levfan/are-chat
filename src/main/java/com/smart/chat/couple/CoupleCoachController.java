package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 成长系（F150-F159）：习惯搭子/感恩便签/情绪颗粒度/每周高光/共读一分钟/拖延互助/早安能量/优点存折/年度关键词。 */
@RestController
@RequestMapping("/api/couple/coach")
public class CoupleCoachController {

    private final CoupleCoachService service;

    public CoupleCoachController(CoupleCoachService service) {
        this.service = service;
    }

    /** 习惯搭子列表。 */
    @GetMapping("/habits")
    public ApiResponse<List<CoupleCoachService.HabitVO>> habits(HttpSession session) {
        return ApiResponse.ok(service.habits(Sessions.requireUser(session)));
    }

    /** 立一个习惯。 */
    public record HabitCreateRequest(String title, Integer targetDays) {
    }

    @PostMapping("/habits")
    public ApiResponse<List<CoupleCoachService.HabitVO>> createHabit(@RequestBody HabitCreateRequest req, HttpSession session) {
        return ApiResponse.ok(service.createHabit(Sessions.requireUser(session), req.title(), req.targetDays()));
    }

    /** 习惯打卡。 */
    @PostMapping("/habits/{id}/checkin")
    public ApiResponse<List<CoupleCoachService.HabitVO>> checkinHabit(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.checkinHabit(Sessions.requireUser(session), id));
    }

    /** 感恩便签墙。 */
    @GetMapping("/thanks")
    public ApiResponse<List<CoupleCoachService.ThanksVO>> thanks(HttpSession session) {
        return ApiResponse.ok(service.thanks(Sessions.requireUser(session)));
    }

    /** 写感恩便签。 */
    public record ThanksRequest(String content) {
    }

    @PostMapping("/thanks")
    public ApiResponse<List<CoupleCoachService.ThanksVO>> addThanks(@RequestBody ThanksRequest req, HttpSession session) {
        return ApiResponse.ok(service.addThanks(Sessions.requireUser(session), req.content()));
    }

    /** 情绪词表。 */
    @GetMapping("/feel-families")
    public ApiResponse<List<CoupleCoachBank.FeelFamily>> feelFamilies() {
        return ApiResponse.ok(CoupleCoachBank.feelFamilies());
    }

    /** 今日情绪日记。 */
    @GetMapping("/feel")
    public ApiResponse<CoupleCoachService.FeelVO> feelToday(HttpSession session) {
        return ApiResponse.ok(service.feelToday(Sessions.requireUser(session)));
    }

    /** 记录今日情绪。 */
    public record FeelSaveRequest(String word, Integer intensity, String note) {
    }

    @PostMapping("/feel")
    public ApiResponse<CoupleCoachService.FeelVO> saveFeel(@RequestBody FeelSaveRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveFeel(Sessions.requireUser(session), req.word(), req.intensity(), req.note()));
    }

    /** 本周高光互评。 */
    @GetMapping("/week-star")
    public ApiResponse<CoupleCoachService.WeekStarVO> weekStar(HttpSession session) {
        return ApiResponse.ok(service.weekStar(Sessions.requireUser(session)));
    }

    /** 提名对方本周高光。 */
    public record WeekStarRequest(String highlight) {
    }

    @PostMapping("/week-star")
    public ApiResponse<CoupleCoachService.WeekStarVO> saveWeekStar(@RequestBody WeekStarRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveWeekStar(Sessions.requireUser(session), req.highlight()));
    }

    /** 今日共读一分钟。 */
    @GetMapping("/read-minute")
    public ApiResponse<CoupleCoachService.ReadMinuteVO> readMinute(HttpSession session) {
        return ApiResponse.ok(service.readMinute(Sessions.requireUser(session)));
    }

    /** 写共读感想。 */
    public record ReadMinuteRequest(String thought) {
    }

    @PostMapping("/read-minute")
    public ApiResponse<CoupleCoachService.ReadMinuteVO> saveReadMinute(@RequestBody ReadMinuteRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveReadMinute(Sessions.requireUser(session), req.thought()));
    }

    /** 拖延互助所。 */
    @GetMapping("/delays")
    public ApiResponse<List<CoupleCoachService.DelayVO>> delayTasks(HttpSession session) {
        return ApiResponse.ok(service.delayTasks(Sessions.requireUser(session)));
    }

    /** 登记拖延的事。 */
    public record DelayRequest(String title, String deadlineDay) {
    }

    @PostMapping("/delays")
    public ApiResponse<List<CoupleCoachService.DelayVO>> addDelay(@RequestBody DelayRequest req, HttpSession session) {
        return ApiResponse.ok(service.addDelay(Sessions.requireUser(session), req.title(), req.deadlineDay()));
    }

    /** 催办。 */
    @PostMapping("/delays/{id}/nag")
    public ApiResponse<List<CoupleCoachService.DelayVO>> nagDelay(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.nagDelay(Sessions.requireUser(session), id));
    }

    /** 宣布完成。 */
    @PostMapping("/delays/{id}/done")
    public ApiResponse<List<CoupleCoachService.DelayVO>> doneDelay(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.doneDelay(Sessions.requireUser(session), id));
    }

    /** 早安能量站。 */
    @GetMapping("/morning")
    public ApiResponse<CoupleCoachService.MorningVO> morning(HttpSession session) {
        return ApiResponse.ok(service.morning(Sessions.requireUser(session)));
    }

    /** 优点存折。 */
    @GetMapping("/praise-bank")
    public ApiResponse<List<CoupleCoachService.PraiseBankVO>> praiseBank(HttpSession session) {
        return ApiResponse.ok(service.praiseBank(Sessions.requireUser(session)));
    }

    /** 存一条优点。 */
    public record PraiseBankRequest(String content, String scene) {
    }

    @PostMapping("/praise-bank")
    public ApiResponse<List<CoupleCoachService.PraiseBankVO>> addPraiseBank(@RequestBody PraiseBankRequest req, HttpSession session) {
        return ApiResponse.ok(service.addPraiseBank(Sessions.requireUser(session), req.content(), req.scene()));
    }

    /** 成长年度关键词。 */
    @GetMapping("/year-keyword")
    public ApiResponse<CoupleCoachService.YearKeywordVO> yearKeyword(@RequestParam(required = false) Integer year, HttpSession session) {
        return ApiResponse.ok(service.yearKeyword(Sessions.requireUser(session), year));
    }
}
