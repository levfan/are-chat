package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.account.Account;
import com.smart.chat.identity.domain.account.AccountRepository;
import com.smart.chat.identity.infrastructure.security.PasswordHasher;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.identity.domain.ProfileProvisioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账号服务：注册建号昵称回退、登录后修改昵称（同步 app_user）。
 * <p>
 * 改造后 mock 的是 {@link AccountRepository} 端口而不是 Mapper，<b>断言的期望值一字未改</b>——
 * 「昵称空白回退成用户名」「空昵称被 400 拒绝」这些口径本来就是行为，只是取数换了接线。
 */
@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AccountRepository accounts;

    @Mock
    private ProfileProvisioner profileProvisioner;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private AppUserService service;

    /** 测试夹具：一个处于 ACTIVE 的既有账号（restore 不做校验，正是夹具需要的） */
    private static Account existing(String username, String nickname) {
        return Account.restore("u-" + username, "13800000001", username, nickname, "hash", "c0",
                Account.STATUS_ACTIVE, Account.ROLE_USER, 1L, null);
    }

    @Test
    void createAccountFallsBackToUsernameWhenNicknameBlank() {
        when(accounts.findByPhone("13900001111")).thenReturn(Optional.empty());
        when(accounts.findByUsername("zhangsan")).thenReturn(Optional.empty());

        service.createAccount("13900001111", "zhangsan", "  ", "hash", Account.ROLE_USER);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(captor.capture());
        assertThat(captor.getValue().nickname()).isEqualTo("zhangsan");
    }

    @Test
    void createAccountUsesProvidedNickname() {
        when(accounts.findByPhone("13900001111")).thenReturn(Optional.empty());
        when(accounts.findByUsername("zhangsan")).thenReturn(Optional.empty());

        service.createAccount("13900001111", "zhangsan", "张三", "hash", Account.ROLE_USER);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(captor.capture());
        assertThat(captor.getValue().nickname()).isEqualTo("张三");

        verify(profileProvisioner).provision(org.mockito.ArgumentMatchers.eq("zhangsan"),
                org.mockito.ArgumentMatchers.eq("张三"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateNicknameTrimsAndSaves() {
        Account alice = existing("alice", "alice");
        when(accounts.findByUsername("alice")).thenReturn(Optional.of(alice));

        service.updateNickname("alice", "  新昵称  ");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(captor.capture());
        assertThat(captor.getValue().nickname()).isEqualTo("新昵称");
    }

    @Test
    void updateNicknameRejectsBlank() {
        when(accounts.findByUsername("alice")).thenReturn(Optional.of(existing("alice", "alice")));

        assertThatThrownBy(() -> service.updateNickname("alice", "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称");
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void updateNicknameRejectsTooLong() {
        when(accounts.findByUsername("alice")).thenReturn(Optional.of(existing("alice", "alice")));

        assertThatThrownBy(() -> service.updateNickname("alice", "x".repeat(33)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称");
    }

    @Test
    void updateNicknameUnknownUserIs404() {
        when(accounts.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNickname("ghost", "新昵称"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号不存在");
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void validateNicknameNormalizesAndValidates() {
        assertThat(service.validateNickname(null)).isNull();
        assertThat(service.validateNickname("   ")).isNull();
        assertThat(service.validateNickname(" 小明 ")).isEqualTo("小明");
        assertThatThrownBy(() -> service.validateNickname("x".repeat(33)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1~32");
    }

    @Test
    void validateNicknameToleratesCjkBoundary() {
        // 32 个汉字恰好合法（昵称按字符数校验）
        assertThat(service.validateNickname("中".repeat(32))).hasSize(32);
    }
}
