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
 * 每日仪式升级：甜蜜任务卡 / 默契大考验 / 情话抽卡 / 恋爱运势 / 晚安故事。
 */
@RestController
@RequestMapping("/api/couple/ritual")
public class CoupleRitualController {

    public record TacitAnswerRequest(String answer) {
    }

    private final CoupleRitualService ritualService;

    public CoupleRitualController(CoupleRitualService ritualService) {
        this.ritualService = ritualService;
    }

    // ---------- 甜蜜任务卡 ----------

    /** 今天的任务卡（没有就生成；重复拉取同一张）。 */
    @GetMapping("/task")
    public ApiResponse<CoupleRitualService.TaskVO> todayTask(HttpSession session) {
        return ApiResponse.ok(ritualService.todayTask(Sessions.requireUser(session)));
    }

    /** 最近 14 天任务卡（双方，新→旧）。 */
    @GetMapping("/tasks")
    public ApiResponse<List<CoupleRitualService.TaskVO>> recentTasks(HttpSession session) {
        return ApiResponse.ok(ritualService.recentTasks(Sessions.requireUser(session)));
    }

    /** 打卡完成今天的任务。 */
    @PostMapping("/task/done")
    public ApiResponse<CoupleRitualService.TaskVO> doneTask(HttpSession session) {
        return ApiResponse.ok(ritualService.doneTask(Sessions.requireUser(session)));
    }

    // ---------- 默契大考验 ----------

    /** 默契状态：进行中的一局 + 累计默契数。 */
    @GetMapping("/tacit")
    public ApiResponse<CoupleRitualService.TacitStateVO> tacitState(HttpSession session) {
        return ApiResponse.ok(ritualService.tacitState(Sessions.requireUser(session)));
    }

    /** 发起一局默契考验。 */
    @PostMapping("/tacit/start")
    public ApiResponse<CoupleRitualService.TacitVO> startTacit(HttpSession session) {
        return ApiResponse.ok(ritualService.startTacit(Sessions.requireUser(session)));
    }

    /** 提交我的答案（第二个人提交后立即结算）。 */
    @PostMapping("/tacit/answer")
    public ApiResponse<CoupleRitualService.TacitVO> answerTacit(@RequestBody TacitAnswerRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(ritualService.answerTacit(Sessions.requireUser(session), req.answer()));
    }

    /** 默契历史（最近 20 局）。 */
    @GetMapping("/tacit/history")
    public ApiResponse<List<CoupleRitualService.TacitVO>> tacitHistory(HttpSession session) {
        return ApiResponse.ok(ritualService.tacitHistory(Sessions.requireUser(session)));
    }

    // ---------- 情话抽卡 / 恋爱运势 / 晚安故事 ----------

    /** 随机抽一句情话。 */
    @GetMapping("/love-word")
    public ApiResponse<String> drawLoveWord(HttpSession session) {
        return ApiResponse.ok(ritualService.drawLoveWord(Sessions.requireUser(session)));
    }

    /** 今日恋爱运势（同一天双方同一张签）。 */
    @GetMapping("/fortune")
    public ApiResponse<CoupleRitualService.FortuneVO> fortune(HttpSession session) {
        return ApiResponse.ok(ritualService.fortune(Sessions.requireUser(session)));
    }

    /** 今晚的晚安故事。 */
    @GetMapping("/goodnight-story")
    public ApiResponse<CoupleRitualService.StoryVO> goodnightStory(HttpSession session) {
        return ApiResponse.ok(ritualService.goodnightStory(Sessions.requireUser(session)));
    }
}
