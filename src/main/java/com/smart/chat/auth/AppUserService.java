package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.FriendMapper;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 用户账号服务：手机号注册申请 → 管理员审批（77）→ 合法用户；登录支持「手机号或用户名 + 密码」。
 * 演示账号播种已移除（模拟用户不能登录，存量演示账号由 AdminBootstrapper 启动时禁用）。
 */
@Service
public class AppUserService {

    /** 中国大陆手机号 */
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    /** 用户名：小写字母/数字/下划线，3~20 位（保证可作为 URL、会话 key 使用） */
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,20}$");

    private final AppUserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final FriendMapper friendMapper;
    private final PasswordHasher passwordHasher;
    private final SmsCodeService smsCodeService;

    public AppUserService(AppUserMapper userMapper, UserProfileMapper profileMapper, FriendMapper friendMapper,
                          PasswordHasher passwordHasher, SmsCodeService smsCodeService) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.friendMapper = friendMapper;
        this.passwordHasher = passwordHasher;
        this.smsCodeService = smsCodeService;
    }

    /** 手机号格式校验（注册与发验证码共用） */
    public String requireValidPhone(String phone) {
        String value = phone == null ? "" : phone.replaceAll("\\s|-", "");
        if (!PHONE.matcher(value).matches()) {
            throw new BusinessException(400, "请输入正确的 11 位手机号");
        }
        return value;
    }

    /** 用户名规范化：去空格 + 转小写（用户名区分大小写没有意义，统一小写避免重复） */
    public String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    /** 规范化登录账号：手机号或用户名都按小写比较 */
    public String normalizeAccount(String account) {
        return account == null ? "" : account.trim().toLowerCase();
    }

    /**
     * 77 创建账号（注册申请审批通过 / 管理员引导时调用；密码必须先经 PasswordHasher 编码）。
     * 原直接注册入口已由 RegistrationService 的审批流取代。
     */
    @Transactional
    public AppUser createAccount(String phone, String username, String passwordHash, String role) {
        String validPhone = requireValidPhone(phone);
        String name = normalizeUsername(username);
        if (!USERNAME.matcher(name).matches()) {
            throw new BusinessException(400, "用户名需为 3~20 位小写字母、数字或下划线");
        }
        if (userMapper.findByPhone(validPhone).isPresent()) {
            throw new BusinessException(409, "该手机号已经注册过了，直接登录吧");
        }
        if (userMapper.findByUsername(name).isPresent()) {
            throw new BusinessException(409, "用户名已被占用，换一个试试");
        }
        AppUser user = AppUser.of(validPhone, name, passwordHash, name, "c0", role);
        userMapper.insert(user);
        ensureProfile(user);
        return user;
    }

    /** 80 修改密码：校验旧密码 + 新密码强度 */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        AppUser user = find(username).orElseThrow(() -> new BusinessException(404, "账号不存在"));
        if (oldPassword == null || !passwordHasher.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "旧密码不正确");
        }
        validatePassword(newPassword);
        if (passwordHasher.matches(newPassword == null ? "" : newPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }
        user.setPasswordHash(passwordHasher.encode(newPassword));
        userMapper.updateById(user);
    }

    /** 84 账号自助注销：标记 CLOSED（保留唯一性占位），清理双向好友关系，不再可登录/被搜索/被加好友 */
    @Transactional
    public void deactivate(String username, String password) {
        AppUser user = find(username).orElseThrow(() -> new BusinessException(404, "账号不存在"));
        if (password == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            throw new BusinessException(400, "密码不正确，无法注销");
        }
        user.setStatus(AppUser.STATUS_CLOSED);
        userMapper.updateById(user);
        friendMapper.deleteAllByOwner(username);
        friendMapper.deleteAllByFriend(username);
    }

    /** 79 管理员启用/禁用用户账号 */
    @Transactional
    public void setStatus(String username, String status) {
        AppUser user = find(username).orElseThrow(() -> new BusinessException(404, "账号不存在"));
        if (!AppUser.STATUS_ACTIVE.equals(status) && !AppUser.STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "状态仅支持 ACTIVE / DISABLED");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /** 79 管理员重置密码：返回一次性展示的新密码 */
    @Transactional
    public String resetPassword(String username, String newPassword) {
        AppUser user = find(username).orElseThrow(() -> new BusinessException(404, "账号不存在"));
        validatePassword(newPassword);
        user.setPasswordHash(passwordHasher.encode(newPassword));
        userMapper.updateById(user);
        return newPassword;
    }

    /** 管理员鉴权：非 ADMIN 一律 403（79 管理控制台入口） */
    public AppUser requireAdmin(String username) {
        AppUser user = find(username)
                .orElseThrow(() -> new BusinessException(401, "账号不存在或已注销"));
        if (!user.isAdmin()) {
            throw new BusinessException(403, "需要管理员权限");
        }
        return user;
    }

    public List<AppUser> admins() {
        return userMapper.findAdmins();
    }

    public boolean hasAdmin() {
        return userMapper.countAdmins() > 0;
    }

    /** 登录：account 可以是手机号或用户名 */
    public AppUser login(String account, String password) {
        String key = normalizeAccount(account);
        if (key.isEmpty()) {
            throw new BusinessException(400, "请输入手机号或用户名");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException(400, "请输入密码");
        }
        AppUser user = userMapper.findByAccount(key)
                .orElseThrow(() -> new BusinessException(401, "账号或密码不正确"));
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "账号或密码不正确");
        }
        if (AppUser.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(403, "该账号已被禁用，联系管理员处理");
        }
        if (AppUser.STATUS_CLOSED.equals(user.getStatus())) {
            throw new BusinessException(403, "该账号已注销，如需使用请重新申请");
        }
        user.setLastLoginAt(System.currentTimeMillis());
        userMapper.updateById(user);
        return user;
    }

    /** 用户是否存在（好友申请、私信等合法性判断的唯一入口） */
    public boolean exists(String username) {
        return username != null && userMapper.findByUsername(normalizeUsername(username)).isPresent();
    }

    public Optional<AppUser> find(String username) {
        return userMapper.findByUsername(normalizeUsername(username));
    }

    /** 输入联想候选：按用户名/手机号匹配，排除自己 */
    public List<AppUser> search(String keyword, String excludeUsername, int limit) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        return userMapper.search(q, limit).stream()
                .filter(u -> !u.getUsername().equals(normalizeUsername(excludeUsername)))
                .toList();
    }

    /** 注册时同步建资料行，保证既有资料/资料卡逻辑可直接使用（RegistrationService 复用） */
    public void ensureProfile(AppUser user) {
        if (profileMapper.selectById(user.getUsername()) != null) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setSignature("");
        profile.setAvatar(user.getAvatar());
        profile.setPresenceStatus("online");
        profile.setUpdatedAt(System.currentTimeMillis());
        profileMapper.insert(profile);
    }

    /** 密码规则：6~64 位、无空格、必须同时含字母和数字（注册申请与改密共用） */
    public void validatePassword(String password) {
        String value = password == null ? "" : password;
        if (value.length() < 6 || value.length() > 64) {
            throw new BusinessException(400, "密码长度需为 6~64 位");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new BusinessException(400, "密码不能包含空格");
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(400, "密码需同时包含字母和数字");
        }
    }
}
