package com.smart.chat.auth;

import com.smart.chat.im.ImPushService;
import com.smart.chat.im.PrivateMessageMapper;
import com.smart.chat.notify.AdminNotifyService;
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

/** 79 用户管理：重置密码支持指定密码与随机临时密码两种方式 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private RegistrationApplicationMapper applicationMapper;

    @Mock
    private AppUserMapper userMapper;

    @Mock
    private AppUserService userService;

    @Mock
    private AdminAuditMapper auditMapper;

    @Mock
    private PrivateMessageMapper messageMapper;

    @Mock
    private ImPushService push;

    @Mock
    private AdminNotifyService notifyService;

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
        verify(auditMapper).insert(captor.capture());
        AdminAudit audit = captor.getValue();
        assertThat(audit.getActor()).isEqualTo("admin");
        assertThat(audit.getAction()).isEqualTo("RESET_PASSWORD");
        assertThat(audit.getTarget()).isEqualTo("bob");
        assertThat(audit.getDetail()).isEqualTo(expectedDetail);
    }
}
