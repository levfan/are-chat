package com.smart.chat.auth;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.BusinessException;
import com.smart.chat.common.Sessions;
import com.smart.chat.config.LoginInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号体系：77 注册审批流（申请 → 管理员审批 → 开通）+ 手机号/用户名密码登录。
 * 演示账号已移除：所有账号都必须经审批产生；模拟用户不能登录。
 * 验证码仍是演示网关：验证码在响应里回显（devCode）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record SmsRequest(String phone) {
    }

    public record SmsResult(String phone, long expiresInSeconds, String devCode, String hint) {
    }

    /** nickname 为注册昵称（选填）：审批通过后作为账号昵称生效 */
    public record RegisterRequest(String phone, String username, String nickname, String password, String code) {
    }

    /** account 支持手机号或用户名；username 字段为兼容旧前端保留 */
    public record LoginRequest(String account, String username, String password) {
    }

    public record LoginResult(String username, String nickname, String phone, String greeting, Long loginAt,
                              String role) {
        static LoginResult of(AppUser user) {
            return new LoginResult(user.getUsername(), user.getNickname(), user.maskedPhone(),
                    "success:欢迎进入 are-chat！", user.getLastLoginAt(),
                    user.getRole() == null ? AppUser.ROLE_USER : user.getRole());
        }
    }

    /** 77 注册申请提交结果：status=PENDING，审批通过后才能登录 */
    public record RegisterResult(String applicationId, String username, String nickname, String status, String hint) {
    }

    /** 77 审批进度查询结果 */
    public record ApplicationStatusVO(String status, String rejectReason, Long created, Long reviewedAt) {
    }

    public record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    /** 84 注销：需密码确认 */
    public record DeactivateRequest(String password) {
    }

    public static final String SESSION_LOGIN_AT = "LoginAt";

    private final AppUserService userService;
    private final SmsCodeService smsCodeService;
    private final LoginRateLimiter rateLimiter;
    private final RegistrationService registrationService;

    public AuthController(AppUserService userService, SmsCodeService smsCodeService, LoginRateLimiter rateLimiter,
                          RegistrationService registrationService) {
        this.userService = userService;
        this.smsCodeService = smsCodeService;
        this.rateLimiter = rateLimiter;
        this.registrationService = registrationService;
    }

    /** 获取注册验证码（演示环境回显 devCode） */
    @PostMapping("/sms-code")
    public ApiResponse<SmsResult> smsCode(@RequestBody SmsRequest req) {
        String phone = userService.requireValidPhone(req == null ? null : req.phone());
        String code = smsCodeService.issue(phone);
        return ApiResponse.ok(new SmsResult(phone, smsCodeService.ttlSeconds(), code,
                "演示环境不发送真实短信，验证码已直接回显"));
    }

    /** 77 注册：提交审批申请（不再直接建号登录），管理员通过后才能登录 */
    @PostMapping("/register")
    public ApiResponse<RegisterResult> register(@RequestBody RegisterRequest req) {
        if (req == null) {
            throw new BusinessException(400, "注册信息不能为空");
        }
        RegistrationService.ApplicationVO application =
                registrationService.apply(req.phone(), req.username(), req.password(), req.code(), req.nickname());
        return ApiResponse.ok(new RegisterResult(application.id(), application.username(), application.nickname(),
                application.status(),
                "申请已提交，请等待管理员审批；审批通过后使用用户名/手机号 + 密码登录"));
    }

    /** 77 查询审批进度：注册页轮询，或审批后登录前确认状态 */
    @GetMapping("/register-status")
    public ApiResponse<ApplicationStatusVO> registerStatus(@RequestParam(required = false) String account) {
        RegistrationService.ApplicationVO application = registrationService.statusByAccount(account)
                .orElseThrow(() -> new BusinessException(404, "没有找到对应的注册申请"));
        return ApiResponse.ok(new ApplicationStatusVO(application.status(), application.rejectReason(),
                application.created(), application.reviewedAt()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@RequestBody(required = false) LoginRequest req, HttpSession session) {
        String account = req == null ? "" : (req.account() == null ? req.username() : req.account());
        String key = userService.normalizeAccount(account);
        if (key.isEmpty()) {
            throw new BusinessException(400, "请输入手机号或用户名");
        }
        if (rateLimiter.isLocked(key)) {
            long seconds = Math.max(1, rateLimiter.retryAfterMs(key) / 1000);
            throw new BusinessException(429, "尝试次数过多，请 " + seconds + " 秒后再试");
        }
        try {
            AppUser user = userService.login(account, req.password());
            rateLimiter.reset(key);
            startSession(session, user);
            return ApiResponse.ok(LoginResult.of(user));
        } catch (BusinessException e) {
            if (e.getCode() == 401) {
                rateLimiter.recordFailure(key);
                // 77 账号不存在但申请在途：给出审批状态提示而不是「密码错误」
                registrationService.loginHint(key)
                        .ifPresent(hint -> { throw new BusinessException(403, hint); });
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<LoginResult> me(HttpSession session) {
        String username = (String) session.getAttribute(LoginInterceptor.SESSION_USER);
        if (username == null) {
            throw new BusinessException(401, "还没有登录哦");
        }
        AppUser user = userService.find(username)
                .orElseThrow(() -> new BusinessException(401, "账号不存在或已注销"));
        Object loginAt = session.getAttribute(SESSION_LOGIN_AT);
        return ApiResponse.ok(new LoginResult(user.getUsername(), user.getNickname(), user.maskedPhone(),
                "success:欢迎回来", loginAt instanceof Long ? (Long) loginAt : null,
                user.getRole() == null ? AppUser.ROLE_USER : user.getRole()));
    }

    /** 80 修改密码：旧密码校验 + 新密码强度校验 */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest req, HttpSession session) {
        String username = Sessions.username(session);
        userService.changePassword(username, req == null ? null : req.oldPassword(),
                req == null ? null : req.newPassword());
        return ApiResponse.ok();
    }

    /** 84 账号自助注销：密码确认后标记 CLOSED，清理双向好友关系并失效会话 */
    @PostMapping("/deactivate")
    public ApiResponse<Void> deactivate(@RequestBody(required = false) DeactivateRequest req, HttpSession session) {
        String username = Sessions.username(session);
        userService.deactivate(username, req == null ? null : req.password());
        session.invalidate();
        return ApiResponse.ok();
    }

    private void startSession(HttpSession session, AppUser user) {
        session.setAttribute(LoginInterceptor.SESSION_USER, user.getUsername());
        session.setAttribute(SESSION_LOGIN_AT, System.currentTimeMillis());
    }
}
