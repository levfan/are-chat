package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import com.smart.chat.notify.AdminNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 77 注册审批工作流：申请 → 管理员审批 → 开通账号。
 * 申请阶段只写 registration_application 表，不产生任何合法用户；
 * 审批通过（AdminService）才把申请搬进 app_user。模拟用户播种已移除。
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    /** 77 注册申请提交结果 */
    public record ApplicationVO(String id, String username, String nickname, String phone, String status,
                                String rejectReason, Long created, Long reviewedAt, String reviewedBy) {
        static ApplicationVO of(RegistrationApplication app) {
            String phone = app.getPhone();
            String masked = phone == null || phone.length() < 7 ? phone
                    : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
            return new ApplicationVO(app.getId(), app.getUsername(), app.getNickname(), masked, app.getStatus(),
                    app.getRejectReason(), app.getCreated(), app.getReviewedAt(), app.getReviewedBy());
        }
    }

    private final RegistrationApplicationMapper applicationMapper;
    private final AppUserService userService;
    private final PasswordHasher passwordHasher;
    private final SmsCodeService smsCodeService;
    private final AdminNotifyService notifyService;
    private final ImPushService push;

    public RegistrationService(RegistrationApplicationMapper applicationMapper, AppUserService userService,
                               PasswordHasher passwordHasher, SmsCodeService smsCodeService,
                               AdminNotifyService notifyService, ImPushService push) {
        this.applicationMapper = applicationMapper;
        this.userService = userService;
        this.passwordHasher = passwordHasher;
        this.smsCodeService = smsCodeService;
        this.notifyService = notifyService;
        this.push = push;
    }

    /** 提交注册申请：校验与原注册一致；申请入待审队列并通知管理员。昵称选填，不填审批后默认用用户名 */
    @Transactional
    public ApplicationVO apply(String phone, String username, String password, String code, String nickname) {
        String validPhone = userService.requireValidPhone(phone);
        String name = userService.normalizeUsername(username);
        userService.validatePassword(password);
        String validNickname = userService.validateNickname(nickname);
        smsCodeService.verifyAndConsume(validPhone, code);

        if (userService.exists(name)) {
            throw new BusinessException(409, "该用户名已经注册过了，直接登录吧");
        }
        if (userService.find(normalizePhoneAsAccount(validPhone)).isPresent()) {
            throw new BusinessException(409, "该手机号已经注册过了，直接登录吧");
        }
        if (applicationMapper.findPendingByUsername(name).isPresent()) {
            throw new BusinessException(409, "该用户名已有待审批的申请，请耐心等待管理员处理");
        }
        if (applicationMapper.findPendingByPhone(validPhone).isPresent()) {
            throw new BusinessException(409, "该手机号已有待审批的申请，请耐心等待管理员处理");
        }

        RegistrationApplication application =
                RegistrationApplication.of(validPhone, name, validNickname, passwordHasher.encode(password));
        applicationMapper.insert(application);
        log.info("新注册申请：username={} nickname={} phone={}", name, validNickname, validPhone);

        // 78 免费渠道推送 + 站内待办（推送失败不影响申请）
        notifyService.pushTextAsync("are-chat 新用户注册申请",
                "**" + name + "**（手机号 " + mask(validPhone) + "）申请加入 are-chat，请到管理后台审批。");
        // 78 在线管理员实时收到待办角标
        push.pushAdminEvent(userService.admins().stream().map(AppUser::getUsername).toList(),
                applicationMapper.countByStatus(RegistrationApplication.STATUS_PENDING));
        return ApplicationVO.of(application);
    }

    /** 按用户名/手机号查询最近一次申请状态（注册页「查询审批进度」） */
    public Optional<ApplicationVO> statusByAccount(String account) {
        String key = userService.normalizeAccount(account);
        if (key.isEmpty()) {
            throw new BusinessException(400, "请输入申请时使用的用户名或手机号");
        }
        return applicationMapper.findLatestByAccount(key).map(ApplicationVO::of);
    }

    /** 登录时的友好提示：账号没建出来但存在申请 → 告知审批状态而不是「密码错误」 */
    public Optional<String> loginHint(String account) {
        return statusByAccount(account).flatMap(app -> switch (app.status()) {
            case RegistrationApplication.STATUS_PENDING ->
                    Optional.of("注册申请正在等待管理员审批，通过后即可登录");
            case RegistrationApplication.STATUS_REJECTED -> Optional.of(
                    app.rejectReason() == null || app.rejectReason().isBlank()
                            ? "注册申请未通过审批，如有疑问联系管理员"
                            : "注册申请未通过：" + app.rejectReason());
            default -> Optional.empty();
        });
    }

    public long countPending() {
        return applicationMapper.countByStatus(RegistrationApplication.STATUS_PENDING);
    }

    private static String normalizePhoneAsAccount(String phone) {
        return phone == null ? "" : phone.trim().toLowerCase();
    }

    private static String mask(String phone) {
        return phone == null || phone.length() < 7 ? phone
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
