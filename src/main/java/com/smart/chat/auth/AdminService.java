package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import com.smart.chat.im.PrivateMessage;
import com.smart.chat.im.PrivateMessageMapper;
import com.smart.chat.notify.AdminNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 77/79 管理员服务：注册申请审批（通过→开通账号+欢迎消息 / 拒绝→留原因）、
 * 用户管理（搜索/禁用启用/重置密码）、操作审计（93）。
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    /** 79 用户管理视图 */
    public record AdminUserVO(String username, String phone, String nickname, String status, String role,
                              Long created, Long lastLoginAt) {
        static AdminUserVO of(AppUser user) {
            return new AdminUserVO(user.getUsername(), user.maskedPhone(), user.getNickname(),
                    user.getStatus(), user.getRole(), user.getCreated(), user.getLastLoginAt());
        }
    }

    /** 93 审计视图 */
    public record AuditVO(String id, String actor, String action, String target, String detail, Long created) {
        static AuditVO of(AdminAudit audit) {
            return new AuditVO(audit.getId(), audit.getActor(), audit.getAction(), audit.getTarget(),
                    audit.getDetail(), audit.getCreated());
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RESET_ALPHABET = "abcdefghjkmnpqrstuvwxyz";
    private static final String RESET_DIGITS = "23456789";

    private final RegistrationApplicationMapper applicationMapper;
    private final AppUserMapper userMapper;
    private final AppUserService userService;
    private final AdminAuditMapper auditMapper;
    private final PrivateMessageMapper messageMapper;
    private final ImPushService push;
    private final AdminNotifyService notifyService;

    public AdminService(RegistrationApplicationMapper applicationMapper, AppUserMapper userMapper,
                        AppUserService userService, AdminAuditMapper auditMapper,
                        PrivateMessageMapper messageMapper, ImPushService push,
                        AdminNotifyService notifyService) {
        this.applicationMapper = applicationMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.auditMapper = auditMapper;
        this.messageMapper = messageMapper;
        this.push = push;
        this.notifyService = notifyService;
    }

    public List<RegistrationApplication> applications(String status) {
        return applicationMapper.findByStatus(status);
    }

    public long pendingCount() {
        return applicationMapper.countByStatus(RegistrationApplication.STATUS_PENDING);
    }

    /** 审批通过：申请 → 正式账号 + 个人资料 + 欢迎消息（93） */
    @Transactional
    public AppUser approve(String applicationId, String reviewer) {
        RegistrationApplication application = requireApplication(applicationId);
        if (!RegistrationApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException(409, "该申请已处理过（" + application.getStatus() + "）");
        }
        AppUser user = userService.createAccount(application.getPhone(), application.getUsername(),
                application.getNickname(), application.getPasswordHash(), AppUser.ROLE_USER);

        application.setStatus(RegistrationApplication.STATUS_APPROVED);
        application.setReviewedAt(System.currentTimeMillis());
        application.setReviewedBy(reviewer);
        applicationMapper.updateById(application);

        sendWelcome(user.getUsername(), reviewer);
        audit(reviewer, "APPROVE", user.getUsername(), "通过注册申请 " + application.getId());
        // 78 审批结果免费渠道推送（失败不影响审批结果）
        notifyService.notifyApplicationReviewed(user.getUsername(), true, reviewer);
        log.info("注册申请已通过：username={} reviewer={}", user.getUsername(), reviewer);
        return user;
    }

    @Transactional
    public void reject(String applicationId, String reviewer, String reason) {
        RegistrationApplication application = requireApplication(applicationId);
        if (!RegistrationApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException(409, "该申请已处理过（" + application.getStatus() + "）");
        }
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() > 200) {
            cleanReason = cleanReason.substring(0, 200);
        }
        application.setStatus(RegistrationApplication.STATUS_REJECTED);
        application.setRejectReason(cleanReason);
        application.setReviewedAt(System.currentTimeMillis());
        application.setReviewedBy(reviewer);
        applicationMapper.updateById(application);
        audit(reviewer, "REJECT", application.getUsername(),
                cleanReason.isEmpty() ? "拒绝注册申请（未填原因）" : "拒绝注册申请：" + cleanReason);
        // 78 审批结果免费渠道推送（失败不影响审批结果）
        notifyService.notifyApplicationReviewed(application.getUsername(), false, reviewer);
    }

    public List<AdminUserVO> users(String keyword) {
        return userMapper.searchAll(keyword, 200).stream().map(AdminUserVO::of).toList();
    }

    /** 禁用/启用（注销用户不可再改状态，避免覆盖 CLOSED 语义） */
    @Transactional
    public void setUserStatus(String actor, String username, boolean active) {
        AppUser user = userService.find(username)
                .orElseThrow(() -> new BusinessException(404, "账号不存在：" + username));
        if (AppUser.STATUS_CLOSED.equals(user.getStatus())) {
            throw new BusinessException(400, "该账号已注销，不能修改状态");
        }
        userService.setStatus(username, active ? AppUser.STATUS_ACTIVE : AppUser.STATUS_DISABLED);
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

    public void audit(String actor, String action, String target, String detail) {
        auditMapper.insert(AdminAudit.of(actor, action, target, detail));
    }

    public List<AuditVO> auditLogs(int limit) {
        return auditMapper.findLatest(limit).stream().map(AuditVO::of).toList();
    }

    /** 在线管理员用户名（WS 审批待办推送的收件人） */
    public Map<String, AppUser> admins() {
        return userService.admins().stream()
                .collect(Collectors.toMap(AppUser::getUsername, Function.identity()));
    }

    private RegistrationApplication requireApplication(String applicationId) {
        RegistrationApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "申请不存在");
        }
        return application;
    }

    /** 93 新用户欢迎消息：以审批人身份发一条系统消息，登录即可见未读 */
    private void sendWelcome(String username, String reviewer) {
        String content = "欢迎加入 are-chat！你的注册申请已由管理员 " + reviewer + " 审批通过。"
                + "入门指引：① 到「好友」页添加好友；② Enter 发送、Shift+Enter 换行，可直接粘贴图片；"
                + "③ 左侧栏可切换皮肤/背景与在线状态。祝你聊得开心！";
        PrivateMessage welcome = PrivateMessage.of(reviewer, username, content, PrivateMessage.TYPE_SYSTEM);
        messageMapper.insert(welcome);
        if (push.isOnline(username)) {
            push.pushDm(welcome);
        }
    }

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
