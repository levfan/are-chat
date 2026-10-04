package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleQuestionService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 每日一问（新卡 `couple-question`）：今日题目、回答与回看。
 */
@RestController
@RequestMapping("/api/couple/question")
public class CoupleQuestionController {

    /** 今天的回答。 */
    public record AnswerRequest(String answer) {
    }

    private final CoupleQuestionService service;

    public CoupleQuestionController(CoupleQuestionService service) {
        this.service = service;
    }

    /** 今日一问：题目 + 我的回答 +（双方都答完才有的）TA 的回答。 */
    @GetMapping("/today")
    public ApiResponse<CoupleQuestionService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** 回答（或改写今天的答案），返回整份今日视图。 */
    @PostMapping("/answer")
    public ApiResponse<CoupleQuestionService.TodayVO> answer(@RequestBody AnswerRequest req, HttpSession session) {
        return ApiResponse.ok(service.answer(Sessions.requireUser(session), req.answer()));
    }

    /** 回看最近 N 天（1-90，默认 14）。 */
    @GetMapping("/history")
    public ApiResponse<CoupleQuestionService.HistoryListVO> history(@RequestParam(required = false) Integer days,
                                                                    HttpSession session) {
        return ApiResponse.ok(service.history(Sessions.requireUser(session), days));
    }
}
