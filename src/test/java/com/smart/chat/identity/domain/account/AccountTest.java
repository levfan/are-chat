package com.smart.chat.identity.domain.account;

import com.smart.chat.identity.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 账号聚合的守卫：状态机、登录闸门、昵称与密码更换。
 * <p>
 * 断言全部锁<b>用户看到的原话</b>和对外状态码——这两样都是产品口径，改造前后必须一模一样。
 */
class AccountTest {

    private Account activeAccount() {
        return Account.restore("u1", "13800000001", "alice", "Alice", "hash", "c0",
                Account.STATUS_ACTIVE, Account.ROLE_USER, 1L, null);
    }

    private Account withStatus(String status) {
        return Account.restore("u1", "13800000001", "alice", "Alice", "hash", "c0",
                status, Account.ROLE_USER, 1L, null);
    }

    // ===== 建号 =====

    @Test
    void registerNormalizesNameAndFallsBackToUsernameForNickname() {
        Account account = Account.register(" 139-0000-1111 ", " ZhangSan ", "  张三  ", "hash", null);

        assertThat(account.phone()).isEqualTo("13900001111");
        assertThat(account.username()).isEqualTo("zhangsan");
        assertThat(account.nickname()).isEqualTo("张三");
        assertThat(account.avatar()).isEqualTo("c0");
        assertThat(account.status()).isEqualTo(Account.STATUS_ACTIVE);
        assertThat(account.role()).isEqualTo(Account.ROLE_USER);
        assertThat(account.id()).isNotBlank();
        assertThat(account.created()).isNotNull();
        assertThat(account.lastLoginAt()).isNull();
    }

    @Test
    void registerKeepsBlankNicknameAsUsername() {
        Account account = Account.register("13900001111", "zhangsan", "   ", "hash", Account.ROLE_ADMIN);

        assertThat(account.nickname()).isEqualTo("zhangsan");
        assertThat(account.role()).isEqualTo(Account.ROLE_ADMIN);
    }

    @Test
    void registerRejectsBadPhoneAndUsernameWithProductWording() {
        assertThatThrownBy(() -> Account.register("12345", "zhangsan", "张三", "hash", null))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("请输入正确的 11 位手机号")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
        assertThatThrownBy(() -> Account.register("13900001111", "ab", "张三", "hash", null))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("用户名需为 3~20 位小写字母、数字或下划线");
        assertThatThrownBy(() -> Account.register("13900001111", "Zhang San", "张三", "hash", null))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("用户名需为 3~20 位小写字母、数字或下划线");
    }

    @Test
    void bootstrapSkipsPhoneFormatForThePlaceholderNumber() {
        // 管理员用占位手机号（不参与登录），所以它必须能建出来——这是启动引导的前提
        Account admin = Account.bootstrap("00000000000", " Admin ", "hash", Account.ROLE_ADMIN);

        assertThat(admin.username()).isEqualTo("admin");
        assertThat(admin.nickname()).isEqualTo("admin");
        assertThat(admin.phone()).isEqualTo("00000000000");
        assertThat(admin.isAdmin()).isTrue();
    }

    // ===== 登录闸门 =====

    @Test
    void activeAccountMaySignIn() {
        activeAccount().assertCanSignIn();
    }

    @Test
    void disabledAndClosedAccountsGetTheirOwnSentence() {
        assertThatThrownBy(() -> withStatus(Account.STATUS_DISABLED).assertCanSignIn())
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该账号已被禁用，联系管理员处理")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
        assertThatThrownBy(() -> withStatus(Account.STATUS_CLOSED).assertCanSignIn())
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该账号已注销，如需使用请重新申请")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
    }

    @Test
    void loginStampIsRecorded() {
        Account account = activeAccount();
        account.recordLogin(1234L);

        assertThat(account.lastLoginAt()).isEqualTo(1234L);
    }

    // ===== 状态迁移 =====

    @Test
    void adminCanToggleBetweenActiveAndDisabled() {
        Account account = activeAccount();

        account.moveToStatus(Account.STATUS_DISABLED);
        assertThat(account.status()).isEqualTo(Account.STATUS_DISABLED);
        account.moveToStatus(Account.STATUS_ACTIVE);
        assertThat(account.status()).isEqualTo(Account.STATUS_ACTIVE);
    }

    @Test
    void adminStatusAcceptsOnlyTwoValues() {
        assertThatThrownBy(() -> activeAccount().moveToStatus(Account.STATUS_CLOSED))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("状态仅支持 ACTIVE / DISABLED");
        assertThatThrownBy(() -> activeAccount().moveToStatus("WHATEVER"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("状态仅支持 ACTIVE / DISABLED");
    }

    @Test
    void closedAccountIsTerminalForAdmins() {
        Account closed = withStatus(Account.STATUS_CLOSED);

        assertThatThrownBy(closed::assertAdminMayChangeStatus)
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该账号已注销，不能修改状态");
        activeAccount().assertAdminMayChangeStatus();
    }

    @Test
    void selfDeactivationIsTheOnlyWayIntoClosed() {
        Account account = activeAccount();

        account.markClosed();

        assertThat(account.status()).isEqualTo(Account.STATUS_CLOSED);
        assertThatThrownBy(account::assertCanSignIn)
                .hasMessage("该账号已注销，如需使用请重新申请");
    }

    // ===== 凭据 =====

    @Test
    void passwordChangeGateUsesTheTwoOriginalSentences() {
        Account account = activeAccount();

        assertThatThrownBy(() -> account.assertOldPasswordMatches(false))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("旧密码不正确");
        assertThatThrownBy(() -> account.assertNewPasswordDiffers(true))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("新密码不能与旧密码相同");

        account.assertOldPasswordMatches(true);
        account.assertNewPasswordDiffers(false);
        account.applyPasswordHash("new-hash");
        assertThat(account.passwordHash()).isEqualTo("new-hash");
    }

    // ===== 昵称与角色 =====

    @Test
    void renameTrimsAndRejectsBlankOrTooLong() {
        Account account = activeAccount();

        account.rename("  新昵称  ");
        assertThat(account.nickname()).isEqualTo("新昵称");

        assertThatThrownBy(() -> account.rename("   "))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("昵称需为 1~32 个字");
        assertThatThrownBy(() -> account.rename("x".repeat(33)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("昵称需为 1~32 个字");
        assertThat(account.nickname()).as("被拒的昵称不能把已生效的改掉").isEqualTo("新昵称");
    }

    @Test
    void nonAdminGetsThePermissionSentence() {
        assertThatThrownBy(() -> activeAccount().assertAdmin())
                .isInstanceOf(RuleViolation.class)
                .hasMessage("需要管理员权限")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);

        Account admin = Account.restore("u9", "13800000009", "root", "root", "hash", "c0",
                Account.STATUS_ACTIVE, Account.ROLE_ADMIN, 1L, null);
        admin.assertAdmin();
        admin.grantAdminRole();
        assertThat(admin.role()).isEqualTo(Account.ROLE_ADMIN);
    }

    // ===== 投影 =====

    @Test
    void phoneIsMaskedForAnythingLeavingIdentity() {
        assertThat(activeAccount().maskedPhone()).isEqualTo("138****0001");
        assertThat(withStatus(Account.STATUS_ACTIVE).maskedPhone()).isEqualTo("138****0001");

        Account shortPhone = Account.restore("u2", "12345", "bob", "bob", "hash", "c0",
                Account.STATUS_ACTIVE, Account.ROLE_USER, 1L, null);
        assertThat(shortPhone.maskedPhone()).as("不足 7 位的老数据原样返回，不抛").isEqualTo("12345");
    }

    @Test
    void statusAndRoleLiteralsMatchTheStoredValues() {
        // 这些字符串是数据库里的存量值，也是对外响应里的字段值，改一个就断链
        assertThat(Account.STATUS_ACTIVE).isEqualTo("ACTIVE");
        assertThat(Account.STATUS_DISABLED).isEqualTo("DISABLED");
        assertThat(Account.STATUS_CLOSED).isEqualTo("CLOSED");
        assertThat(Account.ROLE_USER).isEqualTo("USER");
        assertThat(Account.ROLE_ADMIN).isEqualTo("ADMIN");
    }
}
