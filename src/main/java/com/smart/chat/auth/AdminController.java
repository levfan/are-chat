package com.smart.chat.auth;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.BusinessException;
import com.smart.chat.common.Sessions;
import com.smart.chat.system.Announcement;
import com.smart.chat.system.AnnouncementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 79 管理员控制台接口：注册审批 / 用户管理 / 审计日志 / 公告管理。
 * 所有接口都要求 ADMIN 角色（AppUserService.requireAdmin 兜底鉴权）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record RejectRequest(String reason) {
    }

    public record StatusRequest(Boolean active) {
    }

    public record AnnouncementRequest(String content) {
    }

    /** 77 注册申请视图（不含密码哈希） */
    public record ApplicationVO(String id, String username, String phone, String status, String rejectReason,
                                Long created, Long reviewedAt, String reviewedBy) {
        static ApplicationVO of(RegistrationApplication app) {
            String phone = app.getPhone();
            String masked = phone == null || phone.length() < 7 ? phone
                    : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
            return new ApplicationVO(app.getId(), app.getUsername(), masked, app.getStatus(),
                    app.getRejectReason(), app.getCreated(), app.getReviewedAt(), app.getReviewedBy());
        }
    }

    public record PendingCountVO(long applications) {
    }

    public record ResetPasswordVO(String username, String password) {
    }

    private final AdminService adminService;
    private final AppUserService userService;
    private final AnnouncementService announcementService;

    public AdminController(AdminService adminService, AppUserService userService,
                           AnnouncementService announcementService) {
        this.adminService = adminService;
        this.userService = userService;
        this.announcementService = announcementService;
    }

    // ---------- 注册审批（77） ----------

    @GetMapping("/applications")
    public ApiResponse<List<ApplicationVO>> applications(@RequestParam(required = false) String status,
                                                         HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(adminService.applications(status).stream().map(ApplicationVO::of).toList());
    }

    @PostMapping("/applications/{id}/approve")
    public ApiResponse<Map<String, String>> approve(@PathVariable String id, HttpSession session) {
        String reviewer = requireAdmin(session);
        AppUser user = adminService.approve(id, reviewer);
        return ApiResponse.ok(Map.of("username", user.getUsername(), "status", "APPROVED"));
    }

    @PostMapping("/applications/{id}/reject")
    public ApiResponse<Map<String, String>> reject(@PathVariable String id,
                                                   @RequestBody(required = false) RejectRequest req,
                                                   HttpSession session) {
        String reviewer = requireAdmin(session);
        adminService.reject(id, reviewer, req == null ? null : req.reason());
        return ApiResponse.ok(Map.of("status", "REJECTED"));
    }

    @GetMapping("/pending-count")
    public ApiResponse<PendingCountVO> pendingCount(HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(new PendingCountVO(adminService.pendingCount()));
    }

    // ---------- 用户管理（79） ----------

    @GetMapping("/users")
    public ApiResponse<List<AdminService.AdminUserVO>> users(@RequestParam(required = false) String q,
                                                             HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(adminService.users(q));
    }

    @PostMapping("/users/{username}/status")
    public ApiResponse<Void> setStatus(@PathVariable String username, @RequestBody StatusRequest req,
                                       HttpSession session) {
        String actor = requireAdmin(session);
        if (req == null || req.active() == null) {
            throw new BusinessException(400, "缺少 active 参数");
        }
        adminService.setUserStatus(actor, username, req.active());
        return ApiResponse.ok();
    }

    @PostMapping("/users/{username}/reset-password")
    public ApiResponse<ResetPasswordVO> resetPassword(@PathVariable String username, HttpSession session) {
        String actor = requireAdmin(session);
        String password = adminService.resetPassword(actor, username);
        return ApiResponse.ok(new ResetPasswordVO(username, password));
    }

    // ---------- 审计日志（93） ----------

    @GetMapping("/audit")
    public ApiResponse<List<AdminService.AuditVO>> audit(@RequestParam(defaultValue = "100") int limit,
                                                         HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(adminService.auditLogs(limit));
    }

    // ---------- 公告管理（88） ----------

    @PostMapping("/announcements")
    public ApiResponse<AnnouncementService.AnnouncementVO> publish(@RequestBody AnnouncementRequest req,
                                                                   HttpSession session) {
        String actor = requireAdmin(session);
        return ApiResponse.ok(announcementService.publish(actor, req == null ? null : req.content()));
    }

    @PostMapping("/announcements/{id}/close")
    public ApiResponse<Void> close(@PathVariable String id, HttpSession session) {
        String actor = requireAdmin(session);
        announcementService.close(actor, id);
        return ApiResponse.ok();
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> announcements(HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(announcementService.all());
    }

    private String requireAdmin(HttpSession session) {
        String username = Sessions.requireUser(session);
        userService.requireAdmin(username);
        return username;
    }
}
