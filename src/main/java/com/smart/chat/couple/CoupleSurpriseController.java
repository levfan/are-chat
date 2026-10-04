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
 * 刮刮乐与盲盒（保留卡 `couple-surprise`）：每周自动发券、刮开、送券人核销；装盒与到日开箱。
 */
@RestController
@RequestMapping("/api/couple/surprise")
public class CoupleSurpriseController {

    public record BoxCreateRequest(String kind, String content, String openDay) {
    }

    private final CoupleSurpriseService surpriseService;

    public CoupleSurpriseController(CoupleSurpriseService surpriseService) {
        this.surpriseService = surpriseService;
    }

    // ---------- 爱情刮刮乐 ----------

    /** 我的刮刮乐（自动补发本周的卡）。 */
    @GetMapping("/scratches")
    public ApiResponse<List<CoupleSurpriseService.ScratchVO>> scratches(HttpSession session) {
        return ApiResponse.ok(surpriseService.myScratches(Sessions.requireUser(session)));
    }

    /** 刮开我的券。 */
    @PostMapping("/scratches/{id}/scratch")
    public ApiResponse<CoupleSurpriseService.ScratchVO> scratch(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(surpriseService.scratch(Sessions.requireUser(session), id));
    }

    /** 送券人核销（承诺闭环，兑现即 +5 分归送券人）。 */
    @PostMapping("/scratches/{id}/redeem")
    public ApiResponse<CoupleSurpriseService.ScratchVO> redeemScratch(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(surpriseService.redeemScratch(Sessions.requireUser(session), id));
    }

    // ---------- 恋爱盲盒 ----------

    @GetMapping("/boxes")
    public ApiResponse<List<CoupleSurpriseService.BoxVO>> boxes(HttpSession session) {
        return ApiResponse.ok(surpriseService.boxes(Sessions.requireUser(session)));
    }

    /** 装一个盲盒（最早明天开箱）。 */
    @PostMapping("/boxes")
    public ApiResponse<CoupleSurpriseService.BoxVO> createBox(@RequestBody BoxCreateRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(surpriseService.createBox(Sessions.requireUser(session),
                req.kind(), req.content(), req.openDay()));
    }

    /** 开盲盒（到开箱日才能拆）。 */
    @PostMapping("/boxes/{id}/open")
    public ApiResponse<CoupleSurpriseService.BoxVO> openBox(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(surpriseService.openBox(Sessions.requireUser(session), id));
    }
}
