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

/**
 * 深聊系列（F66-F69）：真心话抽签 / 匿名树洞 / 心灵感应 / 情话储蓄罐。
 */
@RestController
@RequestMapping("/api/couple/talk")
public class CoupleTalkController {

    public record TruthAnswerRequest(String answer) {
    }

    public record WhisperAskRequest(String question, Boolean anonymous) {
    }

    public record WhisperAnswerRequest(String answer) {
    }

    public record TelepathyAnswerRequest(String answer) {
    }

    public record LoveDepositRequest(String content) {
    }

    private final CoupleTalkService talkService;

    public CoupleTalkController(CoupleTalkService talkService) {
        this.talkService = talkService;
    }

    // ---------- F66 真心话 ----------

    /** 今天的真心话：题目 + 双方回答。 */
    @GetMapping("/truth")
    public ApiResponse<CoupleTalkService.TruthTodayVO> truthToday(HttpSession session) {
        return ApiResponse.ok(talkService.truthToday(Sessions.requireUser(session)));
    }

    /** 回答今天的真心话（可修改）。 */
    @PostMapping("/truth")
    public ApiResponse<CoupleTalkService.TruthTodayVO> answerTruth(@RequestBody TruthAnswerRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(talkService.answerTruth(Sessions.requireUser(session), req.answer()));
    }

    /** 双方都答过的真心话存档（1-90 天，默认 30）。 */
    @GetMapping("/truth/history")
    public ApiResponse<List<CoupleTalkService.TruthHistoryVO>> truthHistory(
            @RequestParam(required = false) Integer days, HttpSession session) {
        return ApiResponse.ok(talkService.truthHistory(Sessions.requireUser(session), days));
    }

    // ---------- F67 匿名树洞 ----------

    @GetMapping("/whispers")
    public ApiResponse<List<CoupleTalkService.WhisperVO>> whispers(HttpSession session) {
        return ApiResponse.ok(talkService.whispers(Sessions.requireUser(session)));
    }

    /** 往树洞投一个问题（可选匿名，回答后揭晓）。 */
    @PostMapping("/whispers")
    public ApiResponse<List<CoupleTalkService.WhisperVO>> askWhisper(@RequestBody WhisperAskRequest req,
                                                                     HttpSession session) {
        return ApiResponse.ok(talkService.askWhisper(Sessions.requireUser(session),
                req.question(), req.anonymous() == null || req.anonymous()));
    }

    /** 回答树洞提问（不能自问自答）。 */
    @PostMapping("/whispers/{id}/answer")
    public ApiResponse<List<CoupleTalkService.WhisperVO>> answerWhisper(@PathVariable String id,
                                                                        @RequestBody WhisperAnswerRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(talkService.answerWhisper(Sessions.requireUser(session), id, req.answer()));
    }

    // ---------- F68 心灵感应 ----------

    /** 感应板：当前进行中的轮 + 历史轮 + 今日剩余次数。 */
    @GetMapping("/telepathy")
    public ApiResponse<CoupleTalkService.TelepathyBoardVO> telepathyBoard(HttpSession session) {
        return ApiResponse.ok(talkService.telepathyBoard(Sessions.requireUser(session)));
    }

    /** 发起一轮心灵感应（每天最多 3 轮）。 */
    @PostMapping("/telepathy/start")
    public ApiResponse<CoupleTalkService.TelepathyBoardVO> startTelepathy(HttpSession session) {
        return ApiResponse.ok(talkService.startTelepathy(Sessions.requireUser(session)));
    }

    /** 作答（从选项里选；双方答完自动结算）。 */
    @PostMapping("/telepathy/answer")
    public ApiResponse<CoupleTalkService.TelepathyBoardVO> answerTelepathy(@RequestBody TelepathyAnswerRequest req,
                                                                           HttpSession session) {
        return ApiResponse.ok(talkService.answerTelepathy(Sessions.requireUser(session), req.answer()));
    }

    // ---------- F69 情话储蓄罐 ----------

    /** 我的罐子：在罐里的 / 已作为利息送达的。 */
    @GetMapping("/love-bank")
    public ApiResponse<CoupleTalkService.LoveBankBoardVO> loveBank(HttpSession session) {
        return ApiResponse.ok(talkService.loveBank(Sessions.requireUser(session)));
    }

    /** 存一句情话（TA 只会收到「你存了一句」的通知）。 */
    @PostMapping("/love-bank")
    public ApiResponse<CoupleTalkService.LoveBankBoardVO> depositLove(@RequestBody LoveDepositRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(talkService.depositLove(Sessions.requireUser(session), req.content()));
    }
}
