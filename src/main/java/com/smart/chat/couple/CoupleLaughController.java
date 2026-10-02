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
 * 欢笑银行（F390-F399，批次三十五）：笑点存档/每日一逗/冷笑话结冰榜/尴尬回收站/快乐突袭/
 * 笑点默契考/大笑处方/幽默风格图鉴/欢乐周报/年度欢笑榜。
 * 写接口一律返回整份 LaughVO（GET /bank 的形状），GET /week 与 /year 除外。
 */
@RestController
@RequestMapping("/api/couple/laugh")
public class CoupleLaughController {

    private final CoupleLaughService service;

    public CoupleLaughController(CoupleLaughService service) {
        this.service = service;
    }

    /** 欢笑银行总览（F390-F399 聚合，含本周与当年两份榜单）。 */
    @GetMapping("/bank")
    public ApiResponse<CoupleLaughService.LaughVO> bank(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** F398 欢乐周报（周一锚聚合 + Bank 文案）。 */
    @GetMapping("/week")
    public ApiResponse<CoupleLaughService.WeekVO> week(HttpSession session) {
        return ApiResponse.ok(service.weekReport(Sessions.requireUser(session)));
    }

    /** F399 年度欢笑榜（year 缺省当年）。 */
    @GetMapping("/year")
    public ApiResponse<CoupleLaughService.YearVO> year(@RequestParam(required = false) String year,
                                                       HttpSession session) {
        return ApiResponse.ok(service.yearReport(Sessions.requireUser(session), year));
    }

    /** F390 存一条笑点（day ≤今天、title ≤30、scene ≤100、funLevel 1-5 钳制、每人每天 ≤3 条）。 */
    public record MomentRequest(String day, String title, String culprit, String scene, Integer funLevel) {
    }

    @PostMapping("/moment")
    public ApiResponse<CoupleLaughService.LaughVO> moment(@RequestBody MomentRequest req, HttpSession session) {
        return ApiResponse.ok(service.addMoment(Sessions.requireUser(session), req.day(), req.title(),
                req.culprit(), req.scene(), req.funLevel()));
    }

    /** F390 对方补现场证词（一条只补一次）。 */
    public record WitnessRequest(String id, String witness) {
    }

    @PostMapping("/moment/witness")
    public ApiResponse<CoupleLaughService.LaughVO> momentWitness(@RequestBody WitnessRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(service.witnessMoment(Sessions.requireUser(session), req.id(), req.witness()));
    }

    /** F391 值班的人交今天的节目（轮不到你 400，一天一格）。 */
    public record DailyRequest(String content) {
    }

    @PostMapping("/daily")
    public ApiResponse<CoupleLaughService.LaughVO> daily(@RequestBody DailyRequest req, HttpSession session) {
        return ApiResponse.ok(service.serveDaily(Sessions.requireUser(session), req.content()));
    }

    public record IdRequest(String id) {
    }

    /** F391 对方判分：HAPPY 真笑了 / FLAT 没笑 / FAKE 强撑的笑。 */
    public record JudgeRequest(String id, String verdict) {
    }

    @PostMapping("/daily/judge")
    public ApiResponse<CoupleLaughService.LaughVO> dailyJudge(@RequestBody JudgeRequest req, HttpSession session) {
        return ApiResponse.ok(service.judgeDaily(Sessions.requireUser(session), req.id(), req.verdict()));
    }

    /** F392 丢一条冷笑话（≤80 字，内容查重，每人每天 ≤3 条）。 */
    public record JokeRequest(String content) {
    }

    @PostMapping("/joke")
    public ApiResponse<CoupleLaughService.LaughVO> joke(@RequestBody JokeRequest req, HttpSession session) {
        return ApiResponse.ok(service.addJoke(Sessions.requireUser(session), req.content()));
    }

    /** F392 对方判结没结冰（一条只判一次）。 */
    public record FrozenRequest(String id, Boolean frozen) {
    }

    @PostMapping("/joke/judge")
    public ApiResponse<CoupleLaughService.LaughVO> jokeJudge(@RequestBody FrozenRequest req, HttpSession session) {
        return ApiResponse.ok(service.judgeJoke(Sessions.requireUser(session), req.id(), req.frozen()));
    }

    /** F393 交一条社死往事（day ≤今天、≤100 字、每人每天一条）。 */
    public record CringeRequest(String day, String content) {
    }

    @PostMapping("/cringe")
    public ApiResponse<CoupleLaughService.LaughVO> cringe(@RequestBody CringeRequest req, HttpSession session) {
        return ApiResponse.ok(service.addCringe(Sessions.requireUser(session), req.day(), req.content()));
    }

    /** F393 对方盖「抱抱你」章（满一年后读时自动转成好笑的事）。 */
    @PostMapping("/cringe/heal")
    public ApiResponse<CoupleLaughService.LaughVO> cringeHeal(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.healCringe(Sessions.requireUser(session), req.id()));
    }

    /** F394 发动一次快乐突袭（PRAISE/MEME/MEMORY，每人每天一次）。 */
    public record AttackRequest(String kind, String content) {
    }

    @PostMapping("/attack")
    public ApiResponse<CoupleLaughService.LaughVO> attack(@RequestBody AttackRequest req, HttpSession session) {
        return ApiResponse.ok(service.addAttack(Sessions.requireUser(session), req.kind(), req.content()));
    }

    /** F394 收方「中弹」盖章。 */
    @PostMapping("/attack/hit")
    public ApiResponse<CoupleLaughService.LaughVO> attackHit(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.hitAttack(Sessions.requireUser(session), req.id()));
    }

    /** F395 预判对方会不会笑（一条梗每人一票，可改自己那一票）。 */
    public record GuessRequest(String jokeId, Boolean predict) {
    }

    @PostMapping("/guess")
    public ApiResponse<CoupleLaughService.LaughVO> guess(@RequestBody GuessRequest req, HttpSession session) {
        return ApiResponse.ok(service.guessJoke(Sessions.requireUser(session), req.jokeId(), req.predict()));
    }

    /** F396 开一张大笑处方（指向本空间的一条笑点/社死/突袭，每人每天一张）。 */
    public record RxRequest(String targetKind, String targetId, String note) {
    }

    @PostMapping("/rx")
    public ApiResponse<CoupleLaughService.LaughVO> rx(@RequestBody RxRequest req, HttpSession session) {
        return ApiResponse.ok(service.addRx(Sessions.requireUser(session), req.targetKind(), req.targetId(),
                req.note()));
    }

    /** F396 收方回执「已服用」。 */
    @PostMapping("/rx/taken")
    public ApiResponse<CoupleLaughService.LaughVO> rxTaken(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.takeRx(Sessions.requireUser(session), req.id()));
    }

    /** F397 记一份幽默风格（aboutUser 限两人之一，rater=自己；自评与互评各一行）。 */
    public record StyleRequest(String aboutUser, String style, String note) {
    }

    @PostMapping("/style")
    public ApiResponse<CoupleLaughService.LaughVO> style(@RequestBody StyleRequest req, HttpSession session) {
        return ApiResponse.ok(service.setStyle(Sessions.requireUser(session), req.aboutUser(), req.style(),
                req.note()));
    }
}
