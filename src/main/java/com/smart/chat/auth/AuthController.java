package com.smart.chat.auth;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.BusinessException;
import com.smart.chat.config.LoginInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号体系：手机号注册 → 合法用户；登录用「手机号或用户名 + 密码」。
 * 演示环境没有短信网关，验证码在响应里回显（devCode），前端直接给出提示。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record SmsRequest(String phone) {
    }

    public record SmsResult(String phone, long expiresInSeconds, String devCode, String hint) {
    }

    public record RegisterRequest(String phone, String username, String password, String code) {
    }

    /** account 支持手机号或用户名；username 字段为兼容旧前端保留 */
    public record LoginRequest(String account, String username, String password) {
    }

    public record LoginResult(String username, String nickname, String phone, String greeting, Long loginAt) {
        static LoginResult of(AppUser user) {
            return new LoginResult(user.getUsername(), user.getNickname(), user.maskedPhone(),
                    "success:欢迎进入 are-chat！", user.getLastLoginAt());
        }
    }

    public static final String SESSION_LOGIN_AT = "LoginAt";

    private final AppUserService userService;
    private final SmsCodeService smsCodeService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AppUserService userService, SmsCodeService smsCodeService, LoginRateLimiter rateLimiter) {
        this.userService = userService;
        this.smsCodeService = smsCodeService;
        this.rateLimiter = rateLimiter;
    }

    /** 获取注册验证码（演示环境回显 devCode） */
    @PostMapping("/sms-code")
    public ApiResponse<SmsResult> smsCode(@RequestBody SmsRequest req) {
        String phone = userService.requireValidPhone(req == null ? null : req.phone());
        String code = smsCodeService.issue(phone);
        return ApiResponse.ok(new SmsResult(phone, smsCodeService.ttlSeconds(), code,
                "演示环境不发送真实短信，验证码已直接回显"));
    }

    /** 手机号注册：注册成功即登录 */
    @PostMapping("/register")
    public ApiResponse<LoginResult> register(@RequestBody RegisterRequest req, HttpSession session) {
        if (req == null) {
            throw new BusinessException(400, "注册信息不能为空");
        }
        AppUser user = userService.register(req.phone(), req.username(), req.password(), req.code());
        startSession(session, user);
        // 新账号此前没有登录记录，这里直接返回本次注册时间
        return ApiResponse.ok(new LoginResult(user.getUsername(), user.getNickname(), user.maskedPhone(),
                "success:欢迎进入 are-chat！", System.currentTimeMillis()));
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
                "success:欢迎回来", loginAt instanceof Long ? (Long) loginAt : null));
    }

    private void startSession(HttpSession session, AppUser user) {
        session.setAttribute(LoginInterceptor.SESSION_USER, user.getUsername());
        session.setAttribute(SESSION_LOGIN_AT, System.currentTimeMillis());
    }
}
