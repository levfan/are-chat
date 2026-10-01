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
import java.util.Map;

/**
 * 趣味游戏（F130-F139，批次九）：一百问 / 出题考TA / 心动概率 / 塔罗 /
 * 世界情话课 / 周末盲选 / 情话Battle / 恋爱天气 / 抽象画（骰子为纯前端）。
 */
@RestController
@RequestMapping("/api/couple/play")
public class CouplePlayController {

    public record SurveyRequest(int qNo, String answer) {
    }

    public record QuizRequest(String question) {
    }

    public record QuizAnswerRequest(String answer) {
    }

    public record QuizJudgeRequest(String verdict) {
    }

    public record LoveWordRequest(String word, String meaning) {
    }

    public record BlindRequest(List<String> picks) {
    }

    public record BattleJoinRequest(String content) {
    }

    public record BattleVoteRequest(String toUser) {
    }

    public record ArtRequest(String title, Integer seed) {
    }

    private final CouplePlayService playService;

    public CouplePlayController(CouplePlayService playService) {
        this.playService = playService;
    }

    // ---------- F130 一百问 ----------

    @GetMapping("/survey")
    public ApiResponse<CouplePlayService.SurveyVO> survey(HttpSession session) {
        return ApiResponse.ok(playService.survey(Sessions.requireUser(session)));
    }

    /** 答一题（答完解锁 TA 同题答案）。 */
    @PostMapping("/survey")
    public ApiResponse<CouplePlayService.SurveyVO> answerSurvey(@RequestBody SurveyRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(playService.answerSurvey(Sessions.requireUser(session), req.qNo(), req.answer()));
    }

    // ---------- F131 出题考TA ----------

    @GetMapping("/quizzes")
    public ApiResponse<List<CouplePlayService.QuizVO>> quizzes(HttpSession session) {
        return ApiResponse.ok(playService.quizzes(Sessions.requireUser(session)));
    }

    /** 出一道题考 TA。 */
    @PostMapping("/quizzes")
    public ApiResponse<List<CouplePlayService.QuizVO>> makeQuiz(@RequestBody QuizRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(playService.makeQuiz(Sessions.requireUser(session), req.question()));
    }

    /** 对方作答。 */
    @PostMapping("/quizzes/{id}/answer")
    public ApiResponse<List<CouplePlayService.QuizVO>> answerQuiz(@PathVariable String id,
                                                                  @RequestBody QuizAnswerRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(playService.answerQuiz(Sessions.requireUser(session), id, req.answer()));
    }

    /** 出题人判分。 */
    @PostMapping("/quizzes/{id}/judge")
    public ApiResponse<List<CouplePlayService.QuizVO>> judgeQuiz(@PathVariable String id,
                                                                 @RequestBody QuizJudgeRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(playService.judgeQuiz(Sessions.requireUser(session), id, req.verdict()));
    }

    // ---------- F132 心动概率 ----------

    @GetMapping("/heartbeat")
    public ApiResponse<CouplePlayService.HeartbeatVO> heartbeat(HttpSession session) {
        return ApiResponse.ok(playService.heartbeat(Sessions.requireUser(session)));
    }

    // ---------- F133 塔罗 ----------

    @GetMapping("/tarot")
    public ApiResponse<CouplePlayService.TarotVO> tarot(HttpSession session) {
        return ApiResponse.ok(playService.tarot(Sessions.requireUser(session)));
    }

    // ---------- F134 世界情话课 ----------

    @GetMapping("/love-lesson")
    public ApiResponse<CouplePlayService.LessonVO> loveLesson(HttpSession session) {
        return ApiResponse.ok(playService.loveLesson(Sessions.requireUser(session)));
    }

    /** 收藏一句情话。 */
    @PostMapping("/love-words")
    public ApiResponse<CouplePlayService.LessonVO> collectLoveWord(@RequestBody LoveWordRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(playService.collectLoveWord(Sessions.requireUser(session),
                req.word(), req.meaning()));
    }

    // ---------- F135 周末盲选 ----------

    @GetMapping("/blind")
    public ApiResponse<CouplePlayService.BlindVO> blindPick(HttpSession session) {
        return ApiResponse.ok(playService.blindPick(Sessions.requireUser(session)));
    }

    /** 提交本周 3 个周末愿望。 */
    @PostMapping("/blind")
    public ApiResponse<CouplePlayService.BlindVO> submitBlindPick(@RequestBody BlindRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(playService.submitBlindPick(Sessions.requireUser(session), req.picks()));
    }

    // ---------- F136 情话Battle ----------

    @GetMapping("/battle")
    public ApiResponse<CouplePlayService.BattleVO> battle(HttpSession session) {
        return ApiResponse.ok(playService.battle(Sessions.requireUser(session)));
    }

    /** 参加今日情话 Battle。 */
    @PostMapping("/battle")
    public ApiResponse<CouplePlayService.BattleVO> joinBattle(@RequestBody BattleJoinRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(playService.joinBattle(Sessions.requireUser(session), req.content()));
    }

    /** 投票。 */
    @PostMapping("/battle/vote")
    public ApiResponse<CouplePlayService.BattleVO> voteBattle(@RequestBody BattleVoteRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(playService.voteBattle(Sessions.requireUser(session), req.toUser()));
    }

    // ---------- F137 恋爱天气 ----------

    @GetMapping("/weather")
    public ApiResponse<CouplePlayService.WeatherVO> weather(HttpSession session) {
        return ApiResponse.ok(playService.weather(Sessions.requireUser(session)));
    }

    // ---------- F138 抽象画 ----------

    @GetMapping("/arts")
    public ApiResponse<List<CoupleArtGallery>> arts(HttpSession session) {
        return ApiResponse.ok(playService.arts(Sessions.requireUser(session)));
    }

    /** 送一幅抽象画进画廊。 */
    @PostMapping("/arts")
    public ApiResponse<List<CoupleArtGallery>> createArt(@RequestBody ArtRequest req,
                                                         HttpSession session) {
        return ApiResponse.ok(playService.createArt(Sessions.requireUser(session), req.title(),
                req.seed() == null ? 0 : req.seed()));
    }
}
