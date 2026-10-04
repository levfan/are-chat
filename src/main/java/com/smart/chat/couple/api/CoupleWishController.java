package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleWishService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 愿望清单（新卡 `couple-wish`）：添加愿望、偷偷标记已准备、许愿人确认实现。
 * 写接口一律返回整份清单，前端拿最新视角直接替换。
 */
@RestController
@RequestMapping("/api/couple/wish")
public class CoupleWishController {

    /** 新愿望：标题必填，说明可空，ownerUsername 可空（默认给自己许）。 */
    public record AddRequest(String title, String note, String ownerUsername) {
    }

    /** 愿望 id。 */
    public record WishIdRequest(String id) {
    }

    /** 改补充说明。 */
    public record NoteRequest(String id, String note) {
    }

    private final CoupleWishService service;

    public CoupleWishController(CoupleWishService service) {
        this.service = service;
    }

    /** 愿望清单：「已准备」只对标记人可见。 */
    @GetMapping("/board")
    public ApiResponse<CoupleWishService.WishBoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 添加一条愿望。 */
    @PostMapping("/add")
    public ApiResponse<CoupleWishService.WishBoardVO> add(@RequestBody AddRequest req, HttpSession session) {
        return ApiResponse.ok(service.add(Sessions.requireUser(session), req.title(), req.note(),
                req.ownerUsername()));
    }

    /** 偷偷标记「已准备」（只有对方能标自己许的愿，不推送给许愿人）。 */
    @PostMapping("/prepare")
    public ApiResponse<CoupleWishService.WishBoardVO> prepare(@RequestBody WishIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.prepare(Sessions.requireUser(session), req.id()));
    }

    /** 撤销「已准备」（只有当初点的人能撤）。 */
    @PostMapping("/unprepare")
    public ApiResponse<CoupleWishService.WishBoardVO> unprepare(@RequestBody WishIdRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(service.unprepare(Sessions.requireUser(session), req.id()));
    }

    /** 许愿人确认愿望实现（这时才公开）。 */
    @PostMapping("/fulfill")
    public ApiResponse<CoupleWishService.WishBoardVO> fulfill(@RequestBody WishIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.fulfill(Sessions.requireUser(session), req.id()));
    }

    /** 改补充说明（只有记录人能改）。 */
    @PostMapping("/note")
    public ApiResponse<CoupleWishService.WishBoardVO> note(@RequestBody NoteRequest req, HttpSession session) {
        return ApiResponse.ok(service.updateNote(Sessions.requireUser(session), req.id(), req.note()));
    }

    /** 删除一条愿望（只有记录人能删，已实现的删不掉）。 */
    @PostMapping("/remove")
    public ApiResponse<CoupleWishService.WishBoardVO> remove(@RequestBody WishIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.remove(Sessions.requireUser(session), req.id()));
    }
}
