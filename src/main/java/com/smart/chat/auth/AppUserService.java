package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 用户账号服务：手机号注册 → 合法用户；登录支持「手机号或用户名 + 密码」。
 * 原先的固定体验账号名单已被本服务取代（注册数据是唯一身份来源）。
 */
@Service
public class AppUserService {

    /** 中国大陆手机号 */
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    /** 用户名：小写字母/数字/下划线，3~20 位（保证可作为 URL、会话 key 使用） */
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,20}$");

    private final AppUserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final PasswordHasher passwordHasher;
    private final SmsCodeService smsCodeService;

    public AppUserService(AppUserMapper userMapper, UserProfileMapper profileMapper,
                          PasswordHasher passwordHasher, SmsCodeService smsCodeService) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
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

    @Transactional
    public AppUser register(String phone, String username, String password, String code) {
        String validPhone = requireValidPhone(phone);
        String name = normalizeUsername(username);
        if (!USERNAME.matcher(name).matches()) {
            throw new BusinessException(400, "用户名需为 3~20 位小写字母、数字或下划线");
        }
        validatePassword(password);

        if (userMapper.findByPhone(validPhone).isPresent()) {
            throw new BusinessException(409, "该手机号已经注册过了，直接登录吧");
        }
        if (userMapper.findByUsername(name).isPresent()) {
            throw new BusinessException(409, "用户名已被占用，换一个试试");
        }
        smsCodeService.verifyAndConsume(validPhone, code);

        AppUser user = AppUser.of(validPhone, name, passwordHasher.encode(password), name, "c0");
        userMapper.insert(user);
        ensureProfile(user);
        return user;
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

    /** 注册时同步建资料行，保证既有资料/资料卡逻辑可直接使用 */
    private void ensureProfile(AppUser user) {
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

    private void validatePassword(String password) {
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
