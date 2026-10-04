package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.account.Account;
import com.smart.chat.identity.domain.account.AccountRepository;
import com.smart.chat.identity.domain.account.AccountRules;
import com.smart.chat.identity.infrastructure.security.PasswordHasher;
import com.smart.chat.identity.domain.AccountCascade;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.identity.domain.ProfileProvisioner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.smart.chat.identity.application.DomainRules.guard;
import static com.smart.chat.identity.application.DomainRules.rule;

/**
 * 账号用例编排：手机号注册申请 → 管理员审批（77）→ 合法用户；登录支持「手机号或用户名 + 密码」。
 * <p>
 * 战术改造后这里<b>一行判定都不留</b>：格式与状态规则在 {@link Account}/{@link AccountRules}，
 * 取数与写库经 {@link AccountRepository}，哈希比对是 infrastructure 的能力（{@link PasswordHasher}），
 * Service 只做四件事——取聚合、喂给领域、落库、把结论交给上层。
 * 演示账号播种已移除（模拟用户不能登录，存量演示账号由 AdminBootstrapper 启动时禁用）。
 */
@Service
public class AppUserService {

    private final AccountRepository accounts;
    private final ProfileProvisioner profileProvisioner;
    private final PasswordHasher passwordHasher;
    /** 注销连带清理：谁的数据谁清理（messaging 摘好友边、couple 散空间并推给对方），identity 不再 import 任何别的上下文 */
    private final List<AccountCascade> accountCascades;

    public AppUserService(AccountRepository accounts, ProfileProvisioner profileProvisioner,
                          PasswordHasher passwordHasher, List<AccountCascade> accountCascades) {
        this.accounts = accounts;
        this.profileProvisioner = profileProvisioner;
        this.passwordHasher = passwordHasher;
        this.accountCascades = accountCascades;
    }

    // ===== 规则入口：判定与文案都在领域，这几个方法只是把领域规则挂到用例门面（Controller/RegistrationService 调的就是它们） =====

    /** 手机号格式校验（注册与发验证码共用） */
    public String requireValidPhone(String phone) {
        return rule(() -> AccountRules.requireValidPhone(phone));
    }

    /** 用户名规范化：去空格 + 转小写（用户名区分大小写没有意义，统一小写避免重复） */
    public String normalizeUsername(String username) {
        return AccountRules.normalizeUsername(username);
    }

    /** 规范化登录账号：手机号或用户名都按小写比较 */
    public String normalizeAccount(String account) {
        return AccountRules.normalizeAccount(account);
    }

    /** 昵称校验（个人资料修改共用）：选填，填了就去空格，1~32 个字。空白输入返回 null */
    public String validateNickname(String nickname) {
        return rule(() -> AccountRules.optionalNickname(nickname));
    }

    /** 昵称校验（注册共用）：必填，1~32 个字 */
    public String requireValidNickname(String nickname) {
        return rule(() -> AccountRules.requireNickname(nickname));
    }

    /** 密码规则：6~64 位、无空格、必须同时含字母和数字（注册申请与改密共用） */
    public void validatePassword(String password) {
        guard(() -> AccountRules.requireValidPassword(password));
    }

    // ===== 用例 =====

    /**
     * 77 创建账号（注册申请审批通过 / 管理员引导时调用；密码必须先经 PasswordHasher 编码）。
     * nickname 为注册时填写的昵称，空则回退用户名。
     * 原直接注册入口已由 RegistrationService 的审批流取代。
     * <p>
     * 手机号/用户名是否已被占用要查库，属于用例级唯一性裁决，所以留在这里（与 couple 的
     * 「已有有效空间」同一手法），文案与 409 状态码原样不动。
     */
    @Transactional
    public Account createAccount(String phone, String username, String nickname, String passwordHash, String role) {
        String validPhone = rule(() -> AccountRules.requireValidPhone(phone));
        String name = rule(() -> AccountRules.requireValidUsername(username));
        if (accounts.findByPhone(validPhone).isPresent()) {
            throw new BusinessException(409, "该手机号已经注册过了，直接登录吧");
        }
        if (accounts.findByUsername(name).isPresent()) {
            throw new BusinessException(409, "用户名已被占用，换一个试试");
        }
        Account account = rule(() -> Account.register(validPhone, name, nickname, passwordHash, role));
        accounts.save(account);
        ensureProfile(account);
        return account;
    }

    /** 登录后修改昵称：同步 app_user（管理后台用户列表 / auth/me 使用），user_profile 由 ProfileController 维护 */
    @Transactional
    public void updateNickname(String username, String nickname) {
        Account account = requireAccount(username);
        guard(() -> account.rename(nickname));
        accounts.save(account);
    }

    /** 80 修改密码：校验旧密码 + 新密码强度 */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Account account = requireAccount(username);
        guard(() -> account.assertOldPasswordMatches(
                oldPassword != null && passwordHasher.matches(oldPassword, account.passwordHash())));
        validatePassword(newPassword);
        guard(() -> account.assertNewPasswordDiffers(
                passwordHasher.matches(newPassword == null ? "" : newPassword, account.passwordHash())));
        account.applyPasswordHash(passwordHasher.encode(newPassword));
        accounts.save(account);
    }

    /** 84 账号自助注销：标记 CLOSED（保留唯一性占位），清理双向好友关系，不再可登录/被搜索/被加好友 */
    @Transactional
    public void deactivate(String username, String password) {
        Account account = requireAccount(username);
        if (password == null || !passwordHasher.matches(password, account.passwordHash())) {
            throw new BusinessException(400, "密码不正确，无法注销");
        }
        account.markClosed();
        accounts.save(account);
        accountCascades.forEach(cascade -> cascade.onDeactivated(username));
    }

    /** 79 管理员启用/禁用用户账号 */
    @Transactional
    public void setStatus(String username, String status) {
        Account account = requireAccount(username);
        guard(() -> account.moveToStatus(status));
        accounts.save(account);
    }

    /** 79 管理员重置密码：返回一次性展示的新密码 */
    @Transactional
    public String resetPassword(String username, String newPassword) {
        Account account = requireAccount(username);
        validatePassword(newPassword);
        account.applyPasswordHash(passwordHasher.encode(newPassword));
        accounts.save(account);
        return newPassword;
    }

    /** 管理员鉴权：非 ADMIN 一律 403（79 管理控制台入口） */
    public Account requireAdmin(String username) {
        Account account = accounts.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new BusinessException(401, "账号不存在或已注销"));
        guard(account::assertAdmin);
        return account;
    }

    /** 登录：account 可以是手机号或用户名 */
    public Account login(String account, String password) {
        String key = normalizeAccount(account);
        if (key.isEmpty()) {
            throw new BusinessException(400, "请输入手机号或用户名");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException(400, "请输入密码");
        }
        Account user = accounts.findByAccount(key)
                .orElseThrow(() -> new BusinessException(401, "账号或密码不正确"));
        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new BusinessException(401, "账号或密码不正确");
        }
        guard(user::assertCanSignIn);
        user.recordLogin(System.currentTimeMillis());
        accounts.save(user);
        return user;
    }

    public List<Account> admins() {
        return accounts.listAdmins();
    }

    public boolean hasAdmin() {
        return accounts.countAdmins() > 0;
    }

    // ===== 读 =====

    /** 用户是否存在（好友申请、私信等合法性判断的唯一入口） */
    public boolean exists(String username) {
        return username != null && accounts.findByUsername(normalizeUsername(username)).isPresent();
    }

    public Optional<Account> find(String username) {
        return accounts.findByUsername(normalizeUsername(username));
    }

    /** 输入联想候选：按用户名/手机号匹配，排除自己 */
    public List<Account> search(String keyword, String excludeUsername, int limit) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        return accounts.listSearchCandidates(q, limit).stream()
                .filter(account -> !account.username().equals(normalizeUsername(excludeUsername)))
                .toList();
    }

    /** 注册时同步建资料行，保证既有资料/资料卡逻辑可直接使用（RegistrationService 复用） */
    public void ensureProfile(Account account) {
        profileProvisioner.provision(account.username(), account.nickname(), account.avatar());
    }

    /** 自助入口的统一取数：查不到就是 404「账号不存在」（管理台另有带用户名的那句） */
    private Account requireAccount(String username) {
        return find(username).orElseThrow(() -> new BusinessException(404, "账号不存在"));
    }
}
