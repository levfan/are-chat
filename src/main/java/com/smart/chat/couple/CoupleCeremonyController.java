package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 愿望券本（保留卡 `couple-cere-coupon`）：发券与核销，写接口返回整份总览。
 */
@RestController
@RequestMapping("/api/couple/ceremony")
public class CoupleCeremonyController {

    /** 券面文字。 */
    public record CouponRequest(String title) {
    }

    /** 券行 id。 */
    public record CouponIdRequest(String id) {
    }

    private final CoupleCeremonyService service;

    public CoupleCeremonyController(CoupleCeremonyService service) {
        this.service = service;
    }

    /** 券本总览。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleCeremonyService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(service.overview(Sessions.requireUser(session)));
    }

    /** 发一张愿望券（扣发券人积分）。 */
    @PostMapping("/coupon")
    public ApiResponse<CoupleCeremonyService.OverviewVO> coupon(@RequestBody CouponRequest req, HttpSession session) {
        return ApiResponse.ok(service.issueCoupon(Sessions.requireUser(session), req.title()));
    }

    /** 核销一张愿望券。 */
    @PostMapping("/coupon/use")
    public ApiResponse<CoupleCeremonyService.OverviewVO> couponUse(@RequestBody CouponIdRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(service.useCoupon(Sessions.requireUser(session), req.id()));
    }
}
