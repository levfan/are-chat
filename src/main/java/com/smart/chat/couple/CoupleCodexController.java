package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 我们百科（F280-F289）：词条/默契综艺/TOP10互猜/外号考据/友情测验/足迹/第一眼对视/习惯图鉴/口味变迁/人格双报。 */
@RestController
@RequestMapping("/api/couple/codex")
public class CoupleCodexController {

    private final CoupleCodexService service;

    public CoupleCodexController(CoupleCodexService service) {
        this.service = service;
    }

    /** 百科总览。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleCodexService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(service.overview(Sessions.requireUser(session)));
    }

    /** F280 词条共建。 */
    public record EntryRequest(String term, String definition, String origin, String usageNote) {
    }

    @PostMapping("/entry")
    public ApiResponse<CoupleCodexService.OverviewVO> entry(@RequestBody EntryRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveEntry(Sessions.requireUser(session),
                req.term(), req.definition(), req.origin(), req.usageNote()));
    }

    public record IdRequest(String id) {
    }

    @PostMapping("/entry/remove")
    public ApiResponse<CoupleCodexService.OverviewVO> entryRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeEntry(Sessions.requireUser(session), req.id()));
    }

    /** F281 开一期默契考。 */
    @PostMapping("/quiz/start")
    public ApiResponse<CoupleCodexService.OverviewVO> quizStart(HttpSession session) {
        return ApiResponse.ok(service.startQuiz(Sessions.requireUser(session)));
    }

    /** F281 交五答。 */
    public record QuizAnswerRequest(String answers) {
    }

    @PostMapping("/quiz/answer")
    public ApiResponse<CoupleCodexService.OverviewVO> quizAnswer(@RequestBody QuizAnswerRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(service.answerQuiz(Sessions.requireUser(session), req.answers()));
    }

    /** F282 本人榜。 */
    public record TopListRequest(String category, String items) {
    }

    @PostMapping("/top/list")
    public ApiResponse<CoupleCodexService.OverviewVO> topList(@RequestBody TopListRequest req, HttpSession session) {
        return ApiResponse.ok(service.topList(Sessions.requireUser(session), req.category(), req.items()));
    }

    /** F282 猜对方的榜。 */
    @PostMapping("/top/guess")
    public ApiResponse<CoupleCodexService.OverviewVO> topGuess(@RequestBody TopListRequest req, HttpSession session) {
        return ApiResponse.ok(service.topGuess(Sessions.requireUser(session), req.category(), req.items()));
    }

    /** F283 外号考据。 */
    public record StoryRequest(String nickname, String givenBy, String occasion, String story, String firstUsedDay) {
    }

    @PostMapping("/story")
    public ApiResponse<CoupleCodexService.OverviewVO> story(@RequestBody StoryRequest req, HttpSession session) {
        return ApiResponse.ok(service.story(Sessions.requireUser(session),
                req.nickname(), req.givenBy(), req.occasion(), req.story(), req.firstUsedDay()));
    }

    /** F284 出题。 */
    public record ExamAskRequest(String question, String answer) {
    }

    @PostMapping("/exam")
    public ApiResponse<CoupleCodexService.OverviewVO> examAsk(@RequestBody ExamAskRequest req, HttpSession session) {
        return ApiResponse.ok(service.examAsk(Sessions.requireUser(session), req.question(), req.answer()));
    }

    /** F284 作答。 */
    public record ExamTryRequest(String id, String answer) {
    }

    @PostMapping("/exam/try")
    public ApiResponse<CoupleCodexService.OverviewVO> examTry(@RequestBody ExamTryRequest req, HttpSession session) {
        return ApiResponse.ok(service.examTry(Sessions.requireUser(session), req.id(), req.answer()));
    }

    /** F285 足迹登记。 */
    public record PlaceRequest(String name, String year, String happened, Integer rating) {
    }

    @PostMapping("/place")
    public ApiResponse<CoupleCodexService.OverviewVO> place(@RequestBody PlaceRequest req, HttpSession session) {
        return ApiResponse.ok(service.place(Sessions.requireUser(session),
                req.name(), req.year(), req.happened(), req.rating()));
    }

    /** F286 第一眼对视盲提交。 */
    public record FirstLookRequest(String moment) {
    }

    @PostMapping("/firstlook")
    public ApiResponse<CoupleCodexService.OverviewVO> firstLook(@RequestBody FirstLookRequest req, HttpSession session) {
        return ApiResponse.ok(service.firstLook(Sessions.requireUser(session), req.moment()));
    }

    /** F287 记录 TA 的小习惯。 */
    public record HabitRequest(String habit, String tag) {
    }

    @PostMapping("/habit")
    public ApiResponse<CoupleCodexService.OverviewVO> habit(@RequestBody HabitRequest req, HttpSession session) {
        return ApiResponse.ok(service.habitAdd(Sessions.requireUser(session), req.habit(), req.tag()));
    }

    /** F287 判案：确实/冤枉。 */
    public record HabitVerdictRequest(String id, String verdict) {
    }

    @PostMapping("/habit/verdict")
    public ApiResponse<CoupleCodexService.OverviewVO> habitVerdict(@RequestBody HabitVerdictRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(service.habitVerdict(Sessions.requireUser(session), req.id(), req.verdict()));
    }

    /** F288 口味变迁。 */
    public record TasteRequest(String thing, String beforeText, String nowText, String shiftedDay) {
    }

    @PostMapping("/taste")
    public ApiResponse<CoupleCodexService.OverviewVO> taste(@RequestBody TasteRequest req, HttpSession session) {
        return ApiResponse.ok(service.taste(Sessions.requireUser(session),
                req.thing(), req.beforeText(), req.nowText(), req.shiftedDay()));
    }

    /** F289 人格双报。 */
    public record TypeRequest(String answers) {
    }

    @PostMapping("/type")
    public ApiResponse<CoupleCodexService.OverviewVO> type(@RequestBody TypeRequest req, HttpSession session) {
        return ApiResponse.ok(service.typeReport(Sessions.requireUser(session), req.answers()));
    }
}
