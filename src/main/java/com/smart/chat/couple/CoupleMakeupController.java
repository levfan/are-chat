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

/**
 * 和好系列（F61/F62）：矛盾复盘（和好锦囊）与道歉券。
 */
@RestController
@RequestMapping("/api/couple/makeup")
public class CoupleMakeupController {

    public record PeaceReviewRequest(String myPart, String nextTime) {
    }

    public record SorrySendRequest(String note) {
    }

    public record SorryUseRequest(String usedNote) {
    }

    private final CoupleMakeupService makeupService;

    public CoupleMakeupController(CoupleMakeupService makeupService) {
        this.makeupService = makeupService;
    }

    // ---------- F61 矛盾复盘 ----------

    /** 复盘列表：按天聚合，含未完成的锦囊。 */
    @GetMapping("/reviews")
    public ApiResponse<List<CoupleMakeupService.PeaceDayVO>> reviews(HttpSession session) {
        return ApiResponse.ok(makeupService.reviews(Sessions.requireUser(session)));
    }

    /** 写下今天的复盘（每人每天一份，可修改；双方都写完合成和好锦囊）。 */
    @PostMapping("/reviews")
    public ApiResponse<List<CoupleMakeupService.PeaceDayVO>> saveReview(@RequestBody PeaceReviewRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(makeupService.saveReview(Sessions.requireUser(session), req.myPart(), req.nextTime()));
    }

    // ---------- F62 道歉券 ----------

    @GetMapping("/sorry-tickets")
    public ApiResponse<List<CoupleMakeupService.SorryTicketVO>> sorryTickets(HttpSession session) {
        return ApiResponse.ok(makeupService.sorryTickets(Sessions.requireUser(session)));
    }

    /** 递一张道歉券（同时最多 2 张有效）。 */
    @PostMapping("/sorry-tickets")
    public ApiResponse<List<CoupleMakeupService.SorryTicketVO>> sendSorry(@RequestBody SorrySendRequest req,
                                                                          HttpSession session) {
        return ApiResponse.ok(makeupService.sendSorry(Sessions.requireUser(session), req.note()));
    }

    /** 收下道歉券（可附一句话）。 */
    @PostMapping("/sorry-tickets/{id}/use")
    public ApiResponse<List<CoupleMakeupService.SorryTicketVO>> useSorry(@PathVariable String id,
                                                                         @RequestBody SorryUseRequest req,
                                                                         HttpSession session) {
        return ApiResponse.ok(makeupService.useSorry(Sessions.requireUser(session), id, req.usedNote()));
    }
}
