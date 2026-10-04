package com.smart.chat.identity.domain.account;

import com.smart.chat.identity.domain.RuleViolation;

import java.util.UUID;

/**
 * 账号聚合根：一个<b>可登录的主体</b>（{@code CONTEXT.md} 的「账号」）。
 * <p>
 * 它是「谁」这个问题的唯一答案：昵称怎么改、状态能不能改、密码能不能换、还能不能登录，
 * 判定都在这里，Service 只负责取聚合、调方法、落库。刻意<b>不暴露 setter</b>：
 * <ul>
 *   <li>{@code ACTIVE → DISABLED/CLOSED} 里 CLOSED 是终态（注销保留用户名/手机号占位），
 *       所以管理员的启停必须走 {@link #moveToStatus}，且先过 {@link #assertAdminMayChangeStatus}；</li>
 *   <li>登录闸门按状态给不同的话（禁用 403「联系管理员处理」、注销 403「请重新申请」），
 *       顺序也在聚合里锁定：先验密码（401）再看状态（403）；</li>
 *   <li>{@code signature}/{@code presence_status} 两列不在聚合里——它们的权威副本在
 *       {@code user_profile}（归 messaging），app_user 上只是历史快照，
 *       所以写回时一律不碰（见 {@code AccountRepositoryAdapter}）。</li>
 * </ul>
 * 与持久化模型（{@code AppUserPO}）的换算只发生在 infrastructure 的仓储适配器里。
 */
public final class Account {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
    /** 84 账号自助注销：保留用户名/手机号占位，禁止登录、不可被搜索/加好友 */
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** 内置头像色档默认值（真实头像设置在 user_profile，app_user 只留初始档） */
    public static final String DEFAULT_AVATAR = "c0";

    private final String id;
    private final String username;
    private final String phone;
    private final String avatar;
    private final Long created;
    private String nickname;
    private String passwordHash;
    private String status;
    private String role;
    private Long lastLoginAt;

    private Account(String id, String phone, String username, String nickname, String passwordHash, String avatar,
                    String status, String role, Long created, Long lastLoginAt) {
        this.id = id;
        this.phone = phone;
        this.username = username;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.avatar = avatar == null || avatar.isBlank() ? DEFAULT_AVATAR : avatar;
        this.status = status;
        this.role = role;
        this.created = created;
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 审批通过后正式开号：手机号、用户名都要过格式闸（文案是产品原话）。
     * 唯一性（手机号/用户名是否已被占用）要查库，属于用例编排，不在这里判。
     */
    public static Account register(String phone, String username, String nickname, String passwordHash, String role) {
        String validPhone = AccountRules.requireValidPhone(phone);
        String name = AccountRules.requireValidUsername(username);
        return new Account(UUID.randomUUID().toString(), validPhone, name,
                AccountRules.nicknameOrDefault(nickname, name), passwordHash, DEFAULT_AVATAR,
                STATUS_ACTIVE, AccountRules.roleOrDefault(role), System.currentTimeMillis(), null);
    }

    /**
     * 启动引导用的系统账号（管理员）：手机号是占位串（{@code 00000000000}，不参与登录），
     * 所以<b>不</b>过手机号格式闸——这与改造前 {@code AppUser.of} 直接建行的行为一致。
     */
    public static Account bootstrap(String phone, String username, String passwordHash, String role) {
        String name = AccountRules.normalizeUsername(username);
        return new Account(UUID.randomUUID().toString(), phone, name, name, passwordHash, DEFAULT_AVATAR,
                STATUS_ACTIVE, AccountRules.roleOrDefault(role), System.currentTimeMillis(), null);
    }

    /** 从存储重建：不做任何业务校验，历史数据（含 CLOSED、缺昵称的旧行）必须能读出来 */
    public static Account restore(String id, String phone, String username, String nickname, String passwordHash,
                                 String avatar, String status, String role, Long created, Long lastLoginAt) {
        return new Account(id, phone, username, nickname, passwordHash, avatar, status, role, created, lastLoginAt);
    }

    // ===== 身份与角色 =====

    /** 是不是管理员（能进管理台、能审批注册） */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    /** 不是 ADMIN 就没资格，403 的文案在这里定（管理台每个入口都靠它兜底） */
    public void assertAdmin() {
        if (!isAdmin()) {
            throw RuleViolation.forbidden("需要管理员权限");
        }
    }

    /** 把既有账号提为管理员（仅启动引导在库里一个管理员都没有时用） */
    public void grantAdminRole() {
        this.role = ROLE_ADMIN;
    }

    // ===== 登录 =====

    /**
     * 登录闸门：密码已经比对了（401 在前），这里只判状态。
     * 两种状态各有各的说法，都不许合并成一句「账号不可用」——用户要知道找谁。
     */
    public void assertCanSignIn() {
        if (STATUS_DISABLED.equals(status)) {
            throw RuleViolation.forbidden("该账号已被禁用，联系管理员处理");
        }
        if (STATUS_CLOSED.equals(status)) {
            throw RuleViolation.forbidden("该账号已注销，如需使用请重新申请");
        }
    }

    /** 登录成功后盖时间戳（落库由 Service 做） */
    public void recordLogin(long at) {
        this.lastLoginAt = at;
    }

    // ===== 凭据 =====

    /** 旧密码必须对得上，否则不给换（400，与改造前一致） */
    public void assertOldPasswordMatches(boolean matches) {
        if (!matches) {
            throw RuleViolation.badRequest("旧密码不正确");
        }
    }

    /** 新密码不能等于旧密码（哈希比对由 infrastructure 的 PasswordHasher 做完，这里只裁决结论） */
    public void assertNewPasswordDiffers(boolean sameAsOld) {
        if (sameAsOld) {
            throw RuleViolation.badRequest("新密码不能与旧密码相同");
        }
    }

    /** 换掉密码哈希：传入的必须已是 {@code PasswordHasher} 编码后的串 */
    public void applyPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // ===== 昵称 =====

    /**
     * 改昵称：去空格后必须落在 1~32 个字。
     * 空白与超长共用一句文案（「昵称需为 1~32 个字」），这是改造前 {@code updateNickname} 的口径。
     */
    public void rename(String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty() || trimmed.length() > AccountRules.NICKNAME_MAX) {
            throw RuleViolation.badRequest("昵称需为 1~32 个字");
        }
        this.nickname = trimmed;
    }

    // ===== 状态迁移 =====

    /** 84 自助注销：终态，用户名/手机号继续占位 */
    public void markClosed() {
        this.status = STATUS_CLOSED;
    }

    /** 注销掉的账号不能被管理员改回启用/禁用，否则 CLOSED 的语义就丢了（400） */
    public void assertAdminMayChangeStatus() {
        if (STATUS_CLOSED.equals(status)) {
            throw RuleViolation.badRequest("该账号已注销，不能修改状态");
        }
    }

    /** 管理员启停：只接受 ACTIVE / DISABLED 两个值 */
    public void moveToStatus(String target) {
        if (!STATUS_ACTIVE.equals(target) && !STATUS_DISABLED.equals(target)) {
            throw RuleViolation.badRequest("状态仅支持 ACTIVE / DISABLED");
        }
        this.status = target;
    }

    // ===== 对外投影 =====

    /** 手机号脱敏：138****0001。对外响应只给这个，原文不出 identity */
    public String maskedPhone() {
        return AccountRules.maskPhone(phone);
    }

    // ===== 访问器（记录式短名） =====

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    /** 手机号原文：只在 application 内部用于比对与脱敏，绝不进 VO */
    public String phone() {
        return phone;
    }

    public String nickname() {
        return nickname;
    }

    /** 密码哈希：只交给 {@code PasswordHasher} 比对，绝不进 VO */
    public String passwordHash() {
        return passwordHash;
    }

    public String avatar() {
        return avatar;
    }

    public String status() {
        return status;
    }

    public String role() {
        return role;
    }

    public Long created() {
        return created;
    }

    public Long lastLoginAt() {
        return lastLoginAt;
    }
}
