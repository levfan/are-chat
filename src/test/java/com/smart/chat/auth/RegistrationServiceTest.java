package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import com.smart.chat.notify.AdminNotifyService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 77 注册审批工作流：申请校验、昵称选填、重复申请拦截 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationApplicationMapper applicationMapper;

    @Mock
    private AppUserService userService;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SmsCodeService smsCodeService;

    @Mock
    private AdminNotifyService notifyService;

    @Mock
    private ImPushService push;

    @InjectMocks
    private RegistrationService service;

    @BeforeEach
    void stubBasics() {
        lenient().when(userService.requireValidPhone(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class));
        lenient().when(userService.normalizeUsername(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).trim().toLowerCase());
        lenient().when(userService.exists(anyString())).thenReturn(false);
        lenient().when(userService.find(anyString())).thenReturn(Optional.empty());
        lenient().when(applicationMapper.findPendingByUsername(anyString())).thenReturn(Optional.empty());
        lenient().when(applicationMapper.findPendingByPhone(anyString())).thenReturn(Optional.empty());
        lenient().when(applicationMapper.countByStatus(anyString())).thenReturn(0L);
        lenient().when(passwordHasher.encode(anyString())).thenReturn("hash");
        // 昵称校验逻辑在 AppUserService，这里按真实规则打桩
        lenient().when(userService.validateNickname(any()))
                .thenAnswer(inv -> {
                    String nickname = inv.getArgument(0);
                    if (nickname == null) {
                        return null;
                    }
                    String trimmed = nickname.trim();
                    if (trimmed.isEmpty()) {
                        return null;
                    }
                    if (trimmed.length() > 32) {
                        throw new BusinessException(400, "昵称需为 1~32 个字");
                    }
                    return trimmed;
                });
    }

    private RegistrationService.ApplicationVO apply(String nickname) {
        return service.apply("13900001111", "zhangsan", "abc12345", "123456", nickname);
    }

    @Test
    void applyStoresOptionalNickname() {
        RegistrationService.ApplicationVO vo = apply("张三");

        ArgumentCaptor<RegistrationApplication> captor =
                ArgumentCaptor.forClass(RegistrationApplication.class);
        verify(applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("张三");
        assertThat(vo.nickname()).isEqualTo("张三");
        assertThat(vo.status()).isEqualTo(RegistrationApplication.STATUS_PENDING);
    }

    @Test
    void applyWithoutNicknameLeavesItNull() {
        RegistrationService.ApplicationVO vo = apply(null);

        ArgumentCaptor<RegistrationApplication> captor =
                ArgumentCaptor.forClass(RegistrationApplication.class);
        verify(applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getNickname()).isNull();
        assertThat(vo.nickname()).isNull();
    }

    @Test
    void applyRejectsTooLongNickname() {
        assertThatThrownBy(() -> apply("x".repeat(33)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称");
        verify(applicationMapper, never()).insert(any(RegistrationApplication.class));
    }

    @Test
    void applyNotifiesAdminsWithPendingCount() {
        when(applicationMapper.countByStatus(RegistrationApplication.STATUS_PENDING)).thenReturn(7L);

        apply("张三");

        verify(notifyService).pushTextAsync(anyString(), anyString());
        verify(push).pushAdminEvent(anyList(), anyLong());
    }
}
