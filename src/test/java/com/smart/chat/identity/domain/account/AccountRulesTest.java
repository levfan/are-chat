package com.smart.chat.identity.domain.account;

import com.smart.chat.identity.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 账号格式规则：文案就是产品口径，逐句钉死（改造前它们长在 AppUserService 的方法体里）。
 */
class AccountRulesTest {

    @Test
    void phoneAcceptsSpacesAndDashesButNothingElse() {
        assertThat(AccountRules.requireValidPhone(" 139 0000 1111 ")).isEqualTo("13900001111");
        assertThat(AccountRules.requireValidPhone("139-0000-1111")).isEqualTo("13900001111");

        assertThatThrownBy(() -> AccountRules.requireValidPhone(null))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("请输入正确的 11 位手机号");
        assertThatThrownBy(() -> AccountRules.requireValidPhone("12345678901"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("请输入正确的 11 位手机号");
        assertThatThrownBy(() -> AccountRules.requireValidPhone("1390000111"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("请输入正确的 11 位手机号");
    }

    @Test
    void usernameIsNormalizedThenChecked() {
        assertThat(AccountRules.normalizeUsername("  ZhangSan_1 ")).isEqualTo("zhangsan_1");
        assertThat(AccountRules.normalizeAccount(" 13900001111 ")).isEqualTo("13900001111");
        assertThat(AccountRules.normalizeUsername(null)).isEmpty();

        assertThat(AccountRules.requireValidUsername(" ZhangSan ")).isEqualTo("zhangsan");
        assertThatThrownBy(() -> AccountRules.requireValidUsername("ab"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("用户名需为 3~20 位小写字母、数字或下划线")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
        assertThatThrownBy(() -> AccountRules.requireValidUsername("a".repeat(21)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("用户名需为 3~20 位小写字母、数字或下划线");
        assertThatThrownBy(() -> AccountRules.requireValidUsername("zhang-san"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("用户名需为 3~20 位小写字母、数字或下划线");
    }

    @Test
    void passwordRulesKeepAllThreeSentences() {
        AccountRules.requireValidPassword("abc12345");

        assertThatThrownBy(() -> AccountRules.requireValidPassword("ab1"))
                .isInstanceOf(RuleViolation.class).hasMessage("密码长度需为 6~64 位");
        assertThatThrownBy(() -> AccountRules.requireValidPassword("a".repeat(65)))
                .isInstanceOf(RuleViolation.class).hasMessage("密码长度需为 6~64 位");
        assertThatThrownBy(() -> AccountRules.requireValidPassword(null))
                .isInstanceOf(RuleViolation.class).hasMessage("密码长度需为 6~64 位");
        assertThatThrownBy(() -> AccountRules.requireValidPassword("abc 123"))
                .isInstanceOf(RuleViolation.class).hasMessage("密码不能包含空格");
        assertThatThrownBy(() -> AccountRules.requireValidPassword("abcdefgh"))
                .isInstanceOf(RuleViolation.class).hasMessage("密码需同时包含字母和数字");
        assertThatThrownBy(() -> AccountRules.requireValidPassword("12345678"))
                .isInstanceOf(RuleViolation.class).hasMessage("密码需同时包含字母和数字");
    }

    @Test
    void nicknameOptionalRequiredAndDefaultedHaveDifferentWordings() {
        assertThat(AccountRules.optionalNickname(null)).isNull();
        assertThat(AccountRules.optionalNickname("  ")).isNull();
        assertThat(AccountRules.optionalNickname(" 小明 ")).isEqualTo("小明");
        assertThatThrownBy(() -> AccountRules.optionalNickname("x".repeat(33)))
                .isInstanceOf(RuleViolation.class).hasMessage("昵称需为 1~32 个字");

        // 注册必填时说的是另一句
        assertThatThrownBy(() -> AccountRules.requireNickname("   "))
                .isInstanceOf(RuleViolation.class).hasMessage("请输入昵称（1~32 个字）");
        assertThat(AccountRules.requireNickname("张三")).isEqualTo("张三");

        // 建号时的回退不做长度校验（与改造前 AppUser.of 一致）
        assertThat(AccountRules.nicknameOrDefault("  ", "zhangsan")).isEqualTo("zhangsan");
        assertThat(AccountRules.nicknameOrDefault(null, "zhangsan")).isEqualTo("zhangsan");
        assertThat(AccountRules.nicknameOrDefault(" 张三 ", "zhangsan")).isEqualTo("张三");
    }

    @Test
    void roleAndMaskHelpers() {
        assertThat(AccountRules.roleOrDefault(null)).isEqualTo(Account.ROLE_USER);
        assertThat(AccountRules.roleOrDefault("  ")).isEqualTo(Account.ROLE_USER);
        assertThat(AccountRules.roleOrDefault(Account.ROLE_ADMIN)).isEqualTo(Account.ROLE_ADMIN);

        assertThat(AccountRules.maskPhone("13900001111")).isEqualTo("139****1111");
        assertThat(AccountRules.maskPhone("123456")).isEqualTo("123456");
        assertThat(AccountRules.maskPhone(null)).isNull();
    }
}
