package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 明日邮局（F290-F299）：新年卡/大事进度/改天拍卖/想象中的家/退休计划/许愿井/胶囊接龙/解梦局/愿望台账/未来信用卡。 */
@RestController
@RequestMapping("/api/couple/post")
public class CouplePostController {

    private final CouplePostService service;

    public CouplePostController(CouplePostService service) {
        this.service = service;
    }

    /** 明日邮局总览（含到期放行/逾期下架惰性结算）。 */
    @GetMapping("/box")
    public ApiResponse<CouplePostService.PostVO> box(HttpSession session) {
        return ApiResponse.ok(service.post(Sessions.requireUser(session)));
    }

    /** F290 写今年的新年卡。 */
    public record OathRequest(String content) {
    }

    @PostMapping("/oath")
    public ApiResponse<CouplePostService.PostVO> oath(@RequestBody OathRequest req, HttpSession session) {
        return ApiResponse.ok(service.oath(Sessions.requireUser(session), req.content()));
    }

    /** F291 立大事。 */
    public record BucketRequest(String name, String targetDay, String note) {
    }

    @PostMapping("/bucket")
    public ApiResponse<CouplePostService.PostVO> bucket(@RequestBody BucketRequest req, HttpSession session) {
        return ApiResponse.ok(service.bucketAdd(Sessions.requireUser(session), req.name(), req.targetDay(), req.note()));
    }

    public record BucketIdRequest(String id) {
    }

    @PostMapping("/bucket/abandon")
    public ApiResponse<CouplePostService.PostVO> bucketAbandon(@RequestBody BucketIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.bucketAbandon(Sessions.requireUser(session), req.id()));
    }

    /** F291 拆一步。 */
    public record StepRequest(String bucketId, String text) {
    }

    @PostMapping("/bucket/step")
    public ApiResponse<CouplePostService.PostVO> bucketStep(@RequestBody StepRequest req, HttpSession session) {
        return ApiResponse.ok(service.bucketStepAdd(Sessions.requireUser(session), req.bucketId(), req.text()));
    }

    public record StepIdRequest(String id) {
    }

    @PostMapping("/bucket/step/done")
    public ApiResponse<CouplePostService.PostVO> bucketStepDone(@RequestBody StepIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.bucketStepDone(Sessions.requireUser(session), req.id()));
    }

    /** F292 上拍。 */
    public record ShelfRequest(String thing) {
    }

    @PostMapping("/shelf")
    public ApiResponse<CouplePostService.PostVO> shelf(@RequestBody ShelfRequest req, HttpSession session) {
        return ApiResponse.ok(service.somedayShelf(Sessions.requireUser(session), req.thing()));
    }

    /** F292 认领排期。 */
    public record TakeRequest(String id, String scheduledDay) {
    }

    @PostMapping("/shelf/take")
    public ApiResponse<CouplePostService.PostVO> shelfTake(@RequestBody TakeRequest req, HttpSession session) {
        return ApiResponse.ok(service.somedayTake(Sessions.requireUser(session), req.id(), req.scheduledDay()));
    }

    @PostMapping("/shelf/done")
    public ApiResponse<CouplePostService.PostVO> shelfDone(@RequestBody TakeRequest req, HttpSession session) {
        return ApiResponse.ok(service.somedayDone(Sessions.requireUser(session), req.id()));
    }

    /** F293 梦想家。 */
    public record HomeRequest(String year, String rooms, String windowView, String smell, String corner) {
    }

    @PostMapping("/home")
    public ApiResponse<CouplePostService.PostVO> home(@RequestBody HomeRequest req, HttpSession session) {
        return ApiResponse.ok(service.dreamHome(Sessions.requireUser(session),
                req.year(), req.rooms(), req.windowView(), req.smell(), req.corner()));
    }

    /** F294 退休计划。 */
    public record RetireRequest(String ageBand, String text) {
    }

    @PostMapping("/retire")
    public ApiResponse<CouplePostService.PostVO> retire(@RequestBody RetireRequest req, HttpSession session) {
        return ApiResponse.ok(service.retire(Sessions.requireUser(session), req.ageBand(), req.text()));
    }

    /** F295 井答。 */
    public record WellRequest(String answer) {
    }

    @PostMapping("/well")
    public ApiResponse<CouplePostService.PostVO> well(@RequestBody WellRequest req, HttpSession session) {
        return ApiResponse.ok(service.wellAnswer(Sessions.requireUser(session), req.answer()));
    }

    /** F296 封存一笔未来信。 */
    public record RelayRequest(String content, Integer years) {
    }

    @PostMapping("/relay")
    public ApiResponse<CouplePostService.PostVO> relay(@RequestBody RelayRequest req, HttpSession session) {
        return ApiResponse.ok(service.relaySeal(Sessions.requireUser(session), req.content(), req.years()));
    }

    public record RelayIdRequest(String id) {
    }

    /** F296 拆 TA 写给我的到期笔。 */
    @PostMapping("/relay/open")
    public ApiResponse<CouplePostService.PostVO> relayOpen(@RequestBody RelayIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.relayOpen(Sessions.requireUser(session), req.id()));
    }

    /** F297 记梦。 */
    public record DreamRequest(String dream) {
    }

    @PostMapping("/dream")
    public ApiResponse<CouplePostService.PostVO> dream(@RequestBody DreamRequest req, HttpSession session) {
        return ApiResponse.ok(service.dreamAdd(Sessions.requireUser(session), req.dream()));
    }

    public record ReadRequest(String id, String reading) {
    }

    @PostMapping("/dream/read")
    public ApiResponse<CouplePostService.PostVO> dreamRead(@RequestBody ReadRequest req, HttpSession session) {
        return ApiResponse.ok(service.dreamRead(Sessions.requireUser(session), req.id(), req.reading()));
    }

    public record JudgeRequest(String id, Boolean good) {
    }

    @PostMapping("/dream/judge")
    public ApiResponse<CouplePostService.PostVO> dreamJudge(@RequestBody JudgeRequest req, HttpSession session) {
        return ApiResponse.ok(service.dreamJudge(Sessions.requireUser(session), req.id(), req.good()));
    }

    /** F298 立/改愿望。 */
    public record WishRequest(String year, String wish) {
    }

    @PostMapping("/wish")
    public ApiResponse<CouplePostService.PostVO> wish(@RequestBody WishRequest req, HttpSession session) {
        return ApiResponse.ok(service.wish(Sessions.requireUser(session), req.year(), req.wish()));
    }

    public record WishVerdictRequest(String id, Boolean kept) {
    }

    @PostMapping("/wish/verdict")
    public ApiResponse<CouplePostService.PostVO> wishVerdict(@RequestBody WishVerdictRequest req, HttpSession session) {
        return ApiResponse.ok(service.wishVerdict(Sessions.requireUser(session), req.id(), req.kept()));
    }

    /** F299 立旗。 */
    public record PromiseRequest(String promise, String dueDay) {
    }

    @PostMapping("/promise")
    public ApiResponse<CouplePostService.PostVO> promise(@RequestBody PromiseRequest req, HttpSession session) {
        return ApiResponse.ok(service.creditPromise(Sessions.requireUser(session), req.promise(), req.dueDay()));
    }

    public record KeepRequest(String id) {
    }

    @PostMapping("/promise/keep")
    public ApiResponse<CouplePostService.PostVO> promiseKeep(@RequestBody KeepRequest req, HttpSession session) {
        return ApiResponse.ok(service.creditKeep(Sessions.requireUser(session), req.id()));
    }
}
