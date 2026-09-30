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
 * 会说情话·沟通增强（F100-F109，批次六）：恋爱翻译器 / 冷静角 / 情绪接力棒 /
 * 你比划我猜 / 故事接龙 / 专属词典小考 / 情话合成器 / 道歉三部曲 / 情绪词汇足迹 / 晚安电台。
 */
@RestController
@RequestMapping("/api/couple/comm")
public class CoupleCommController {

    public record CoolStartRequest(String reason) {
    }

    public record CoolSoftenRequest(String content) {
    }

    public record RelayTossRequest(String moodWord, String moodEmoji, String note) {
    }

    public record RelayCatchRequest(String catchNote, String myMood, String myEmoji, String myNote) {
    }

    public record GuessClueRequest(String clue) {
    }

    public record GuessRequest(String word) {
    }

    public record StoryStartRequest(String content) {
    }

    public record StoryLineRequest(String content) {
    }

    public record ApologyRequest(String whatWrong, String whyWrong, String willDo) {
    }

    public record FeelingRequest(String word, String note) {
    }

    private final CoupleCommService commService;

    public CoupleCommController(CoupleCommService commService) {
        this.commService = commService;
    }

    // ---------- F100 恋爱翻译器 ----------

    /** 翻译 TA 的短语：潜台词 + 建议回应。 */
    @GetMapping("/translate")
    public ApiResponse<CoupleChatBank.Translation> translate(@RequestParam String text, HttpSession session) {
        Sessions.requireUser(session);
        return ApiResponse.ok(commService.translate(text));
    }

    // ---------- F101 冷静角 ----------

    @GetMapping("/cool-downs")
    public ApiResponse<List<CoupleCoolDown>> coolDowns(HttpSession session) {
        return ApiResponse.ok(commService.coolDowns(Sessions.requireUser(session)));
    }

    /** 发起冷静角（同时仅一场）。 */
    @PostMapping("/cool-downs")
    public ApiResponse<List<CoupleCoolDown>> startCoolDown(@RequestBody(required = false) CoolStartRequest req,
                                                           HttpSession session) {
        return ApiResponse.ok(commService.startCoolDown(Sessions.requireUser(session),
                req == null ? null : req.reason()));
    }

    /** 冷静期结束后留一句软话；双方都留即和好。 */
    @PostMapping("/cool-downs/{id}/soften")
    public ApiResponse<List<CoupleCoolDown>> soften(@PathVariable String id,
                                                    @RequestBody CoolSoftenRequest req, HttpSession session) {
        return ApiResponse.ok(commService.soften(Sessions.requireUser(session), id, req.content()));
    }

    // ---------- F102 情绪接力棒 ----------

    @GetMapping("/relays")
    public ApiResponse<List<CoupleMoodRelay>> relays(HttpSession session) {
        return ApiResponse.ok(commService.relays(Sessions.requireUser(session)));
    }

    /** 把心情抛给 TA。 */
    @PostMapping("/relays")
    public ApiResponse<List<CoupleMoodRelay>> tossRelay(@RequestBody RelayTossRequest req, HttpSession session) {
        return ApiResponse.ok(commService.tossRelay(Sessions.requireUser(session),
                req.moodWord(), req.moodEmoji(), req.note()));
    }

    /** 接住 TA 的接力棒，回应并回抛自己的心情。 */
    @PostMapping("/relays/{id}/catch")
    public ApiResponse<List<CoupleMoodRelay>> catchRelay(@PathVariable String id,
                                                         @RequestBody RelayCatchRequest req, HttpSession session) {
        return ApiResponse.ok(commService.catchRelay(Sessions.requireUser(session), id,
                req.catchNote(), req.myMood(), req.myEmoji(), req.myNote()));
    }

    // ---------- F103 你比划我猜 ----------

    @GetMapping("/guesses")
    public ApiResponse<List<CoupleGuessRound>> guesses(HttpSession session) {
        return ApiResponse.ok(commService.guessRounds(Sessions.requireUser(session)));
    }

    /** 开一轮（每天最多 5 轮）。 */
    @PostMapping("/guesses")
    public ApiResponse<List<CoupleGuessRound>> startGuess(HttpSession session) {
        return ApiResponse.ok(commService.startGuess(Sessions.requireUser(session)));
    }

    /** 比划人出提示（不能包含原词）。 */
    @PostMapping("/guesses/{id}/clue")
    public ApiResponse<List<CoupleGuessRound>> clueGuess(@PathVariable String id,
                                                         @RequestBody GuessClueRequest req, HttpSession session) {
        return ApiResponse.ok(commService.clueGuess(Sessions.requireUser(session), id, req.clue()));
    }

    /** 猜词人猜词（错满 3 次结算）。 */
    @PostMapping("/guesses/{id}/guess")
    public ApiResponse<List<CoupleGuessRound>> doGuess(@PathVariable String id,
                                                       @RequestBody GuessRequest req, HttpSession session) {
        return ApiResponse.ok(commService.doGuess(Sessions.requireUser(session), id, req.word()));
    }

    // ---------- F104 故事接龙 ----------

    @GetMapping("/stories")
    public ApiResponse<List<CoupleCommService.StoryVO>> stories(HttpSession session) {
        return ApiResponse.ok(commService.stories(Sessions.requireUser(session)));
    }

    /** 开新故事（有连载中不能开新的）。 */
    @PostMapping("/stories")
    public ApiResponse<List<CoupleCommService.StoryVO>> startStory(@RequestBody StoryStartRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(commService.startStory(Sessions.requireUser(session), req.content()));
    }

    /** 接一句（轮到对方才能接）。 */
    @PostMapping("/stories/{chainId}/lines")
    public ApiResponse<List<CoupleCommService.StoryVO>> addStoryLine(@PathVariable String chainId,
                                                                     @RequestBody StoryLineRequest req,
                                                                     HttpSession session) {
        return ApiResponse.ok(commService.addStoryLine(Sessions.requireUser(session), chainId, req.content()));
    }

    /** 完结本篇。 */
    @PostMapping("/stories/{chainId}/finish")
    public ApiResponse<List<CoupleCommService.StoryVO>> finishStory(@PathVariable String chainId,
                                                                    HttpSession session) {
        return ApiResponse.ok(commService.finishStory(Sessions.requireUser(session), chainId));
    }

    // ---------- F105 专属词典小考 ----------

    /** 出一道词典选择题（前端判分）。 */
    @GetMapping("/dict-quiz")
    public ApiResponse<CoupleCommService.DictQuizVO> dictQuiz(HttpSession session) {
        return ApiResponse.ok(commService.dictQuiz(Sessions.requireUser(session)));
    }

    // ---------- F106 情话合成器 ----------

    /** 合成一句专属情话（seed 随机重摇）。 */
    @GetMapping("/sweet-synth")
    public ApiResponse<String> sweetSynth(@RequestParam(defaultValue = "0") long seed, HttpSession session) {
        return ApiResponse.ok(commService.synthSweet(Sessions.requireUser(session), seed));
    }

    // ---------- F107 道歉三部曲 ----------

    @GetMapping("/apologies")
    public ApiResponse<List<CoupleApologyCard>> apologies(HttpSession session) {
        return ApiResponse.ok(commService.apologies(Sessions.requireUser(session)));
    }

    /** 送出一张道歉三部曲。 */
    @PostMapping("/apologies")
    public ApiResponse<List<CoupleApologyCard>> sendApology(@RequestBody ApologyRequest req, HttpSession session) {
        return ApiResponse.ok(commService.sendApology(Sessions.requireUser(session),
                req.whatWrong(), req.whyWrong(), req.willDo()));
    }

    /** TA 收下道歉。 */
    @PostMapping("/apologies/{id}/accept")
    public ApiResponse<List<CoupleApologyCard>> acceptApology(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(commService.acceptApology(Sessions.requireUser(session), id));
    }

    // ---------- F108 情绪词汇足迹 ----------

    @GetMapping("/feelings")
    public ApiResponse<List<CoupleFeelingWord>> feelings(HttpSession session) {
        return ApiResponse.ok(commService.feelings(Sessions.requireUser(session)));
    }

    /** 记录/更新今天的心情词（每人每天一词）。 */
    @PostMapping("/feelings")
    public ApiResponse<List<CoupleFeelingWord>> saveFeeling(@RequestBody FeelingRequest req, HttpSession session) {
        return ApiResponse.ok(commService.saveFeeling(Sessions.requireUser(session), req.word(), req.note()));
    }

    // ---------- F109 晚安电台 ----------

    /** 今晚的电台：从歌单抽一首 + 晚安语。 */
    @GetMapping("/goodnight-radio")
    public ApiResponse<CoupleCommService.RadioVO> goodnightRadio(HttpSession session) {
        return ApiResponse.ok(commService.goodnightRadio(Sessions.requireUser(session)));
    }
}
