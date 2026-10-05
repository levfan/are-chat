package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 情侣空间地基：邀请建立 → 空间本体（在一起的日子 / 宣言 / 主题 / 爱称）→ 恋爱等级（心动值）。
 * 2026-10-05 二轮裁剪后本类只剩地基，四张功能卡各有自己的 Controller（streak/question/wish/memory）。
 */
@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    public record InviteRequest(String username, String message) {
    }

    public record AnniversaryDateRequest(String date) {
    }

    /** 空间个性化：宣言 / 主题 / 给 TA 的爱称（传 null 表示该项不修改，传空串表示清除）。 */
    public record ProfileUpdateRequest(String slogan, String theme, String petName) {
    }

    private final CoupleService coupleService;

    public CoupleController(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    // ---------- 建立流程 ----------

    /** 总览：未建立时返回待处理邀请（指引建立）；建立后返回空间与在一起天数。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(coupleService.overview(Sessions.requireUser(session)));
    }

    /** 向一位好友发起情侣空间邀请（对方同意后空间才存在）。 */
    @PostMapping("/invites")
    public ApiResponse<CoupleService.InviteVO> invite(@RequestBody InviteRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.invite(Sessions.requireUser(session), req.username(), req.message()));
    }

    /** 同意邀请 → 情侣空间开启。 */
    @PostMapping("/invites/{id}/accept")
    public ApiResponse<CoupleService.SpaceVO> accept(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.accept(Sessions.requireUser(session), id));
    }

    /** 婉拒发给自己的邀请。 */
    @PostMapping("/invites/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable String id, HttpSession session) {
        coupleService.reject(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    /** 撤回自己发出的待处理邀请。 */
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

    /** 空间个性化：我们的宣言 / 空间主题 / 给对方起的爱称。 */
    @PutMapping("/profile")
    public ApiResponse<CoupleService.SpaceVO> updateProfile(@RequestBody ProfileUpdateRequest req,
                                                            HttpSession session) {
        return ApiResponse.ok(coupleService.updateProfile(Sessions.requireUser(session),
                req.slogan(), req.theme(), req.petName()));
    }

    /** F44 恋爱中徽章：查某人是否在恋爱中 + 在一起天数（好友资料卡展示）。 */
    @GetMapping("/relationship-of/{username}")
    public ApiResponse<CoupleService.RelationshipVO> relationshipOf(@PathVariable String username,
                                                                    HttpSession session) {
        return ApiResponse.ok(coupleService.relationshipOf(Sessions.requireUser(session), username));
    }

    /** 解除情侣空间。 */
    @PostMapping("/dissolve")
    public ApiResponse<Void> dissolve(HttpSession session) {
        coupleService.dissolve(Sessions.requireUser(session));
        return ApiResponse.ok();
    }

    // ---------- 心动值 & 恋爱等级 ----------

    @GetMapping("/intimacy")
    public ApiResponse<CoupleService.IntimacyVO> intimacy(HttpSession session) {
        return ApiResponse.ok(coupleService.intimacy(Sessions.requireUser(session)));
    }
}
