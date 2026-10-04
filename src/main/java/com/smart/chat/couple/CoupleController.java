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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 情侣空间地基：邀请建立 → 纪念日 → 空间个性化 → 心情日记 → 心动值。
 * 约定/清单/条约/基金/城市/信箱/时光轴/每日一问 随功能裁剪全部下线。
 */
@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    public record InviteRequest(String username, String message) {
    }

    public record AnniversaryDateRequest(String date) {
    }

    public record AnniversaryCreateRequest(String title, String date, Boolean yearly, String kind,
                                           String calendarType, String lunarMd) {
    }

    public record MoodSaveRequest(String mood, String note) {
    }

    /** 空间个性化：宣言 / 主题 / 贴纸墙（传 null 表示该项不修改）。 */
    public record ProfileUpdateRequest(String slogan, String theme, String stickers) {
    }

    private final CoupleService coupleService;

    public CoupleController(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    // ---------- 建立流程 ----------

    /** 总览：未建立时返回待处理邀请（指引建立）；建立后返回空间与今天双方的心情。 */
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

    /** 空间个性化：我们的宣言 / 空间主题 / 贴纸墙佩戴。 */
    @PutMapping("/profile")
    public ApiResponse<CoupleService.SpaceVO> updateProfile(@RequestBody ProfileUpdateRequest req,
                                                            HttpSession session) {
        return ApiResponse.ok(coupleService.updateProfile(Sessions.requireUser(session),
                req.slogan(), req.theme(), req.stickers()));
    }

    /** F44 恋爱中徽章：查某人是否在恋爱中 + 在一起天数（好友资料卡展示）。 */
    @GetMapping("/relationship-of/{username}")
    public ApiResponse<CoupleService.RelationshipVO> relationshipOf(@PathVariable String username,
                                                                    HttpSession session) {
        return ApiResponse.ok(coupleService.relationshipOf(Sessions.requireUser(session), username));
    }

    @PostMapping("/dissolve")
    public ApiResponse<Void> dissolve(HttpSession session) {
        coupleService.dissolve(Sessions.requireUser(session));
        return ApiResponse.ok();
    }

    // ---------- 共同日历 ----------

    @GetMapping("/anniversaries")
    public ApiResponse<List<CoupleService.AnniversaryVO>> anniversaries(HttpSession session) {
        return ApiResponse.ok(coupleService.listAnniversaries(Sessions.requireUser(session)));
    }

    @PostMapping("/anniversaries")
    public ApiResponse<CoupleService.AnniversaryVO> createAnniversary(@RequestBody AnniversaryCreateRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(coupleService.createAnniversary(Sessions.requireUser(session),
                req.title(), req.date(), req.yearly(), req.kind(), req.calendarType(), req.lunarMd()));
    }

    @DeleteMapping("/anniversaries/{id}")
    public ApiResponse<Void> deleteAnniversary(@PathVariable String id, HttpSession session) {
        coupleService.deleteAnniversary(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 心情日记 ----------

    /** 记录/修改今天的心情（每人每天一条，重复提交视为修改）。 */
    @PostMapping("/moods")
    public ApiResponse<CoupleService.MoodVO> saveMood(@RequestBody MoodSaveRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.saveMood(Sessions.requireUser(session), req.mood(), req.note()));
    }

    /** 双方最近 N 天的心情（1-90，默认 14），按日期新→旧。 */
    @GetMapping("/moods")
    public ApiResponse<List<CoupleService.MoodDayVO>> moods(@RequestParam(defaultValue = "14") int days,
                                                            HttpSession session) {
        return ApiResponse.ok(coupleService.listMoods(Sessions.requireUser(session), days));
    }

    // ---------- 心动值 & 恋爱等级 ----------

    @GetMapping("/intimacy")
    public ApiResponse<CoupleService.IntimacyVO> intimacy(HttpSession session) {
        return ApiResponse.ok(coupleService.intimacy(Sessions.requireUser(session)));
    }
}
