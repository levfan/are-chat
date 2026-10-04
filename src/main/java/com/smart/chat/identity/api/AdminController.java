package com.smart.chat.identity.api;

import com.smart.chat.identity.application.AdminService;
import com.smart.chat.identity.application.AppUserService;
import com.smart.chat.identity.domain.account.Account;
import com.smart.chat.identity.domain.registration.RegistrationApplication;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.sharedkernel.web.Sessions;
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
 * 79 管理员控制台接口：注册审批 / 用户管理 / 审计日志（公告管理已归 platform 的 AnnouncementAdminController）。
 * 所有接口都要求 ADMIN 角色（AppUserService.requireAdmin 兜底鉴权）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record RejectRequest(String reason) {
    }

    public record StatusRequest(Boolean active) {
    }

    /** 79 重置密码请求：password 选填，填了则重置为指定密码，留空则生成随机临时密码 */
    public record ResetPasswordRequest(String password) {
    }

    /** 77 注册申请视图（不含密码哈希；nickname 为注册时填写的昵称） */
    public record ApplicationVO(String id, String username, String nickname, String phone, String status,
                                String rejectReason, Long created, Long reviewedAt, String reviewedBy) {
        static ApplicationVO of(RegistrationApplication app) {
            return new ApplicationVO(app.id(), app.username(), app.nickname(), app.maskedPhone(), app.status(),
                    app.rejectReason(), app.created(), app.reviewedAt(), app.reviewedBy());
        }
    }

    public record PendingCountVO(long applications) {
    }

    public record ResetPasswordVO(String username, String password) {
    }

    private final AdminService adminService;
    private final AppUserService userService;

    public AdminController(AdminService adminService, AppUserService userService) {
        this.adminService = adminService;
        this.userService = userService;
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
        Account user = adminService.approve(id, reviewer);
        return ApiResponse.ok(Map.of("username", user.username(), "status", "APPROVED"));
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
    public ApiResponse<ResetPasswordVO> resetPassword(@PathVariable String username,
                                                      @RequestBody(required = false) ResetPasswordRequest req,
                                                      HttpSession session) {
        String actor = requireAdmin(session);
        String password = adminService.resetPassword(actor, username, req == null ? null : req.password());
        return ApiResponse.ok(new ResetPasswordVO(username, password));
    }

    // ---------- 审计日志（93） ----------

    @GetMapping("/audit")
    public ApiResponse<List<AdminService.AuditVO>> audit(@RequestParam(defaultValue = "100") int limit,
                                                         HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(adminService.auditLogs(limit));
    }

    private String requireAdmin(HttpSession session) {
        String username = Sessions.requireUser(session);
        userService.requireAdmin(username);
        return username;
    }
}
