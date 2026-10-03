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

    /** 欢笑银行总览（笑点存档 / 冷笑话结冰榜 / 社死往事）。 */
    @GetMapping("/bank")
    public ApiResponse<CoupleLaughService.LaughVO> bank(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
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

    public record IdRequest(String id) {
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

}
