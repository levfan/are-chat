package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 情侣空间：邀请建立 → 双向约定 → 每日小仪式 → 共享空间。
 */
@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    public record InviteRequest(String username, String message) {
    }

    public record AnniversaryDateRequest(String date) {
    }

    public record PromiseCreateRequest(String side, String content, Long dueAt) {
    }

    public record CheckinRequest(String kind) {
    }

    public record AnswerRequest(String answer) {
    }

    public record ItemSaveRequest(String kind, String title, String note, String dueDate, Boolean done) {
    }

    public record AnniversaryCreateRequest(String title, String date, Boolean yearly) {
    }

    private final CoupleService coupleService;

    public CoupleController(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    // ---------- 建立流程 ----------

    /** 总览：未建立时返回待处理邀请（指引建立）；建立后返回空间、双方仪式状态与逾期数。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(coupleService.overview(Sessions.requireUser(session)));
    }

    @PostMapping("/invites")
    public ApiResponse<CoupleService.InviteVO> invite(@RequestBody InviteRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.invite(Sessions.requireUser(session), req.username(), req.message()));
    }

    @PostMapping("/invites/{id}/accept")
    public ApiResponse<CoupleService.SpaceVO> accept(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.accept(Sessions.requireUser(session), id));
    }

    @PostMapping("/invites/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable String id, HttpSession session) {
        coupleService.reject(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/invites/{id}")
    public ApiResponse<Void> cancel(@PathVariable String id, HttpSession session) {
        coupleService.cancel(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    /** 在一起纪念日（用于计算在一起天数，双方都可改）。 */
    @PutMapping("/anniversary")
    public ApiResponse<CoupleService.SpaceVO> setAnniversary(@RequestBody AnniversaryDateRequest req,
                                                             HttpSession session) {
        return ApiResponse.ok(coupleService.setAnniversary(Sessions.requireUser(session), req.date()));
    }

    @PostMapping("/dissolve")
    public ApiResponse<Void> dissolve(HttpSession session) {
        coupleService.dissolve(Sessions.requireUser(session));
        return ApiResponse.ok();
    }

    // ---------- 1. 双向待办 / 约定 ----------

    @GetMapping("/promises")
    public ApiResponse<List<CoupleService.PromiseVO>> promises(HttpSession session) {
        return ApiResponse.ok(coupleService.listPromises(Sessions.requireUser(session)));
    }

    @PostMapping("/promises")
    public ApiResponse<CoupleService.PromiseVO> createPromise(@RequestBody PromiseCreateRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(coupleService.createPromise(Sessions.requireUser(session),
                req.side(), req.content(), req.dueAt()));
    }

    @PostMapping("/promises/{id}/done")
    public ApiResponse<CoupleService.PromiseVO> donePromise(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.donePromise(Sessions.requireUser(session), id));
    }

    @PostMapping("/promises/{id}/undone")
    public ApiResponse<CoupleService.PromiseVO> undonePromise(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.undonePromise(Sessions.requireUser(session), id));
    }

    @DeleteMapping("/promises/{id}")
    public ApiResponse<Void> deletePromise(@PathVariable String id, HttpSession session) {
        coupleService.deletePromise(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 2. 每日小仪式 ----------

    @PostMapping("/checkins")
    public ApiResponse<CoupleService.CheckinStateVO> checkin(@RequestBody CheckinRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.checkin(Sessions.requireUser(session), req.kind()));
    }

    @GetMapping("/question")
    public ApiResponse<CoupleService.QuestionVO> question(HttpSession session) {
        return ApiResponse.ok(coupleService.todayQuestion(Sessions.requireUser(session)));
    }

    @PostMapping("/question")
    public ApiResponse<CoupleService.QuestionVO> answer(@RequestBody AnswerRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.answerQuestion(Sessions.requireUser(session), req.answer()));
    }

    // ---------- 3. 共享空间 ----------

    @GetMapping("/items")
    public ApiResponse<List<CoupleService.ItemVO>> items(HttpSession session) {
        return ApiResponse.ok(coupleService.listItems(Sessions.requireUser(session)));
    }

    @PostMapping("/items")
    public ApiResponse<CoupleService.ItemVO> createItem(@RequestBody ItemSaveRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.createItem(Sessions.requireUser(session),
                req.kind(), req.title(), req.note(), req.dueDate()));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CoupleService.ItemVO> updateItem(@PathVariable String id, @RequestBody ItemSaveRequest req,
                                                        HttpSession session) {
        return ApiResponse.ok(coupleService.updateItem(Sessions.requireUser(session), id,
                req.title(), req.note(), req.dueDate(), req.done()));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable String id, HttpSession session) {
        coupleService.deleteItem(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    @GetMapping("/anniversaries")
    public ApiResponse<List<CoupleService.AnniversaryVO>> anniversaries(HttpSession session) {
        return ApiResponse.ok(coupleService.listAnniversaries(Sessions.requireUser(session)));
    }

    @PostMapping("/anniversaries")
    public ApiResponse<CoupleService.AnniversaryVO> createAnniversary(@RequestBody AnniversaryCreateRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(coupleService.createAnniversary(Sessions.requireUser(session),
                req.title(), req.date(), req.yearly()));
    }

    @DeleteMapping("/anniversaries/{id}")
    public ApiResponse<Void> deleteAnniversary(@PathVariable String id, HttpSession session) {
        coupleService.deleteAnniversary(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }
}
