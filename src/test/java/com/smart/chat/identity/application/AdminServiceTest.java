package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.account.AccountRepository;
import com.smart.chat.identity.domain.audit.AdminAudit;
import com.smart.chat.identity.domain.audit.AdminAuditRepository;
import com.smart.chat.identity.domain.registration.RegistrationApplicationRepository;
import com.smart.chat.identity.domain.AdminNotifyChannel;
import com.smart.chat.identity.domain.WelcomeMessenger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 79 用户管理：重置密码支持指定密码与随机临时密码两种方式。
 * mock 从三个 Mapper 换成三个仓储端口，审计断言的期望文本一字未改。
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private RegistrationApplicationRepository applications;

    @Mock
    private AccountRepository accounts;

    @Mock
    private AppUserService userService;

    @Mock
    private AdminAuditRepository audits;

    @Mock
    private WelcomeMessenger welcomeMessenger;

    @Mock
    private AdminNotifyChannel notifyChannel;

    @InjectMocks
    private AdminService service;

    @Test
    void resetPasswordUsesSpecifiedPasswordAndAudits() {
        when(userService.resetPassword(eq("bob"), eq("abc123xyz"))).thenReturn("abc123xyz");

        String password = service.resetPassword("admin", "bob", "abc123xyz");

        assertThat(password).isEqualTo("abc123xyz");
        verify(userService).resetPassword("bob", "abc123xyz");
        assertAudit("重置用户密码（指定密码）");
    }

    @Test
    void resetPasswordGeneratesRandomWhenPasswordBlank() {
        when(userService.resetPassword(eq("bob"), any(String.class)))
                .thenAnswer(inv -> inv.getArgument(1, String.class));

        String password = service.resetPassword("admin", "bob", "");

        // 随机临时密码：10 位，小写字母与数字（去掉易混淆字符）
        assertThat(password).hasSize(10).matches("[a-z2-9]{10}");
        verify(userService).resetPassword("bob", password);
        assertAudit("重置用户密码");
    }

    @Test
    void resetPasswordTreatsNullAsRandom() {
        when(userService.resetPassword(eq("bob"), any(String.class)))
                .thenAnswer(inv -> inv.getArgument(1, String.class));

        String password = service.resetPassword("admin", "bob", null);

        assertThat(password).hasSize(10).matches("[a-z2-9]{10}");
        verify(userService).resetPassword("bob", password);
        assertAudit("重置用户密码");
    }

    private void assertAudit(String expectedDetail) {
        ArgumentCaptor<AdminAudit> captor = ArgumentCaptor.forClass(AdminAudit.class);
        verify(audits).append(captor.capture());
        AdminAudit audit = captor.getValue();
        assertThat(audit.actor()).isEqualTo("admin");
        assertThat(audit.action()).isEqualTo("RESET_PASSWORD");
        assertThat(audit.target()).isEqualTo("bob");
        assertThat(audit.detail()).isEqualTo(expectedDetail);
    }
}
