package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.account.Account;
import com.smart.chat.identity.domain.account.AccountRepository;
import com.smart.chat.identity.domain.audit.AdminAudit;
import com.smart.chat.identity.domain.audit.AdminAuditRepository;
import com.smart.chat.identity.domain.registration.RegistrationApplication;
import com.smart.chat.identity.domain.registration.RegistrationApplicationRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.identity.domain.WelcomeMessenger;
import com.smart.chat.identity.domain.AdminNotifyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.smart.chat.identity.application.DomainRules.guard;

/**
 * 77/79 管理员用例编排：注册申请审批（通过→开通账号+欢迎消息 / 拒绝→留原因）、
 * 用户管理（搜索/禁用启用/重置密码）、操作审计（93）。
 * <p>
 * 「一份申请只能被处理一次」这条状态机闸门在 {@link RegistrationApplication} 里，
 * 「注销账号不可再改状态」在 {@link Account} 里，这里只把它们串起来并把结果投成 VO / 写审计。
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    /** 79 用户管理视图 */
    public record AdminUserVO(String username, String phone, String nickname, String status, String role,
                              Long created, Long lastLoginAt) {
        static AdminUserVO of(Account account) {
            return new AdminUserVO(account.username(), account.maskedPhone(), account.nickname(),
                    account.status(), account.role(), account.created(), account.lastLoginAt());
        }
    }

    /** 93 审计视图 */
    public record AuditVO(String id, String actor, String action, String target, String detail, Long created) {
        static AuditVO of(AdminAudit audit) {
            return new AuditVO(audit.id(), audit.actor(), audit.action(), audit.target(),
                    audit.detail(), audit.created());
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RESET_ALPHABET = "abcdefghjkmnpqrstuvwxyz";
    private static final String RESET_DIGITS = "23456789";

    private final RegistrationApplicationRepository applications;
    private final AccountRepository accounts;
    private final AppUserService userService;
    private final AdminAuditRepository audits;
    private final WelcomeMessenger welcomeMessenger;
    private final AdminNotifyChannel notifyChannel;

    public AdminService(RegistrationApplicationRepository applications, AccountRepository accounts,
                        AppUserService userService, AdminAuditRepository audits,
                        WelcomeMessenger welcomeMessenger,
                        AdminNotifyChannel notifyChannel) {
        this.applications = applications;
        this.accounts = accounts;
        this.userService = userService;
        this.audits = audits;
        this.welcomeMessenger = welcomeMessenger;
        this.notifyChannel = notifyChannel;
    }

    // ===== 注册审批（77） =====

    public List<RegistrationApplication> applications(String status) {
        return applications.listByStatus(status);
    }

    public long pendingCount() {
        return applications.countPending();
    }

    /** 审批通过：申请 → 正式账号 + 个人资料 + 欢迎消息（93） */
    @Transactional
    public Account approve(String applicationId, String reviewer) {
        RegistrationApplication application = requireApplication(applicationId);
        long now = System.currentTimeMillis();
        guard(() -> application.approve(reviewer, now));
        Account user = userService.createAccount(application.phone(), application.username(),
                application.nickname(), application.passwordHash(), Account.ROLE_USER);

        applications.save(application);

        sendWelcome(user.username(), reviewer);
        audit(reviewer, "APPROVE", user.username(), "通过注册申请 " + application.id());
        // 78 审批结果免费渠道推送（失败不影响审批结果）
        notifyChannel.applicationReviewed(user.username(), true, reviewer);
        log.info("注册申请已通过：username={} reviewer={}", user.username(), reviewer);
        return user;
    }

    @Transactional
    public void reject(String applicationId, String reviewer, String reason) {
        RegistrationApplication application = requireApplication(applicationId);
        guard(() -> application.reject(reviewer, reason, System.currentTimeMillis()));
        applications.save(application);
        String cleanReason = application.rejectReason();
        audit(reviewer, "REJECT", application.username(),
                cleanReason.isEmpty() ? "拒绝注册申请（未填原因）" : "拒绝注册申请：" + cleanReason);
        // 78 审批结果免费渠道推送（失败不影响审批结果）
        notifyChannel.applicationReviewed(application.username(), false, reviewer);
    }

    // ===== 用户管理（79） =====

    public List<AdminUserVO> users(String keyword) {
        return accounts.listForAdmin(keyword, 200).stream().map(AdminUserVO::of).toList();
    }

    /** 禁用/启用（注销用户不可再改状态，避免覆盖 CLOSED 语义） */
    @Transactional
    public void setUserStatus(String actor, String username, boolean active) {
        Account user = userService.find(username)
                .orElseThrow(() -> new BusinessException(404, "账号不存在：" + username));
        guard(user::assertAdminMayChangeStatus);
        userService.setStatus(username, active ? Account.STATUS_ACTIVE : Account.STATUS_DISABLED);
        audit(actor, active ? "ENABLE" : "DISABLE", username, active ? "启用账号" : "禁用账号");
    }

    /**
     * 79 重置密码：password 指定时重置为该密码（需通过密码强度校验），为空则生成随机临时密码，
     * 返回给管理员一次性转交。
     */
    @Transactional
    public String resetPassword(String actor, String username, String specifiedPassword) {
        boolean specified = specifiedPassword != null && !specifiedPassword.isEmpty();
        String password = specified ? specifiedPassword : generatePassword();
        userService.resetPassword(username, password);
        audit(actor, "RESET_PASSWORD", username, specified ? "重置用户密码（指定密码）" : "重置用户密码");
        return password;
    }

    // ===== 审计（93） =====

    public void audit(String actor, String action, String target, String detail) {
        audits.append(AdminAudit.written(actor, action, target, detail));
    }

    public List<AuditVO> auditLogs(int limit) {
        return audits.listLatest(limit).stream().map(AuditVO::of).toList();
    }

    /** 在线管理员用户名（WS 审批待办推送的收件人） */
    public Map<String, Account> admins() {
        return userService.admins().stream()
                .collect(Collectors.toMap(Account::username, Function.identity()));
    }

    private RegistrationApplication requireApplication(String applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请不存在"));
    }

    /** 93 新用户欢迎消息：以审批人身份发一条系统消息，登录即可见未读 */
    private void sendWelcome(String username, String reviewer) {
        String content = "欢迎加入小帆船！你的注册申请已由管理员 " + reviewer + " 审批通过。"
                + "入门指引：① 到「好友」页添加好友；② Enter 发送、Shift+Enter 换行，可直接粘贴图片；"
                + "③ 「我的」菜单可快切在线状态，个人中心里有皮肤/背景等外观设置。祝你聊得开心！";
        welcomeMessenger.sendSystemWelcome(reviewer, username, content);
    }

    /** 随机临时密码：6 位易认字母 + 2 位易认数字 + 1 字母 + 1 数字，共 10 位 */
    private static String generatePassword() {
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 6; i++) {
            password.append(RESET_ALPHABET.charAt(RANDOM.nextInt(RESET_ALPHABET.length())));
        }
        for (int i = 0; i < 2; i++) {
            password.append(RESET_DIGITS.charAt(RANDOM.nextInt(RESET_DIGITS.length())));
        }
        password.append(RESET_ALPHABET.charAt(RANDOM.nextInt(RESET_ALPHABET.length())));
        password.append(RESET_DIGITS.charAt(RANDOM.nextInt(RESET_DIGITS.length())));
        return password.toString();
    }
}
