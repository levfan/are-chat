package com.smart.chat.identity.domain.account;

import com.smart.chat.identity.domain.RuleViolation;

import java.util.regex.Pattern;

/**
 * 账号的格式规则：手机号、用户名、昵称、密码，以及大小写/空白口径。
 * <p>
 * 这些规则原先长在 {@code AppUserService} 的方法体里，被注册、改资料、审批、管理台四处各调一遍；
 * 搬进领域后只有这一份实现，文案一字未改——<b>「请输入正确的 11 位手机号」这类句子是产品口径，
 * 也是用例断言的期望值</b>。无状态，所以是策略类而不是聚合。
 */
public final class AccountRules {

    /** 中国大陆手机号 */
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    /** 用户名：小写字母/数字/下划线，3~20 位（保证可作为 URL、会话 key 使用） */
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,20}$");

    /** 昵称上限（按字符数，中文一个字算一个） */
    public static final int NICKNAME_MAX = 32;
    /** 密码长度下限 */
    public static final int PASSWORD_MIN = 6;
    /** 密码长度上限 */
    public static final int PASSWORD_MAX = 64;

    private AccountRules() {
    }

    /** 用户名规范化：去空格 + 转小写（用户名区分大小写没有意义，统一小写避免重复） */
    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    /** 规范化登录账号：手机号或用户名都按小写比较 */
    public static String normalizeAccount(String account) {
        return account == null ? "" : account.trim().toLowerCase();
    }

    /** 手机号格式校验（注册与发验证码共用）：允许输入里带空格和连字符，校验前剔掉 */
    public static String requireValidPhone(String phone) {
        String value = phone == null ? "" : phone.replaceAll("\\s|-", "");
        if (!PHONE.matcher(value).matches()) {
            throw RuleViolation.badRequest("请输入正确的 11 位手机号");
        }
        return value;
    }

    /** 先规范化再验格式：不合法就按产品原话拒绝 */
    public static String requireValidUsername(String username) {
        String name = normalizeUsername(username);
        if (!USERNAME.matcher(name).matches()) {
            throw RuleViolation.badRequest("用户名需为 3~20 位小写字母、数字或下划线");
        }
        return name;
    }

    /** 密码规则：6~64 位、无空格、必须同时含字母和数字（注册申请与改密共用） */
    public static void requireValidPassword(String password) {
        String value = password == null ? "" : password;
        if (value.length() < PASSWORD_MIN || value.length() > PASSWORD_MAX) {
            throw RuleViolation.badRequest("密码长度需为 6~64 位");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw RuleViolation.badRequest("密码不能包含空格");
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw RuleViolation.badRequest("密码需同时包含字母和数字");
        }
    }

    /** 昵称校验（个人资料修改共用）：选填，填了就去空格，1~32 个字；空白输入返回 null */
    public static String optionalNickname(String nickname) {
        if (nickname == null) {
            return null;
        }
        String trimmed = nickname.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > NICKNAME_MAX) {
            throw RuleViolation.badRequest("昵称需为 1~32 个字");
        }
        return trimmed;
    }

    /** 昵称校验（注册共用）：必填，1~32 个字 */
    public static String requireNickname(String nickname) {
        String valid = optionalNickname(nickname);
        if (valid == null) {
            throw RuleViolation.badRequest("请输入昵称（1~32 个字）");
        }
        return valid;
    }

    /**
     * 建号时的昵称：空白回退成用户名，非空白去空格。
     * <p>
     * 刻意<b>不</b>做长度校验——建号入口的昵称长度早已在注册申请时校过，管理员引导路径填的就是用户名。
     */
    public static String nicknameOrDefault(String nickname, String fallback) {
        return nickname == null || nickname.isBlank() ? fallback : nickname.trim();
    }

    /** 角色回退：留空一律按普通用户 */
    public static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? Account.ROLE_USER : role;
    }

    /** 手机号脱敏：138****0001（管理台列表与申请视图共用同一口径） */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
