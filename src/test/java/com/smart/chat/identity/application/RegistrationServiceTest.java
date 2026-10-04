package com.smart.chat.identity.application;

import com.smart.chat.identity.domain.registration.RegistrationApplication;
import com.smart.chat.identity.domain.registration.RegistrationApplicationRepository;
import com.smart.chat.identity.infrastructure.security.PasswordHasher;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.identity.domain.AdminAlerter;
import com.smart.chat.identity.domain.AdminNotifyChannel;
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

/**
 * 77 注册审批工作流：申请校验、昵称必填、重复申请拦截。
 * <p>
 * 接线从 {@code RegistrationApplicationMapper} 换成 {@link RegistrationApplicationRepository} 端口，
 * 所有期望值（文案、状态字面量、脱敏后的手机号）保持原样。
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationApplicationRepository applications;

    @Mock
    private AppUserService userService;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SmsCodeService smsCodeService;

    @Mock
    private AdminNotifyChannel notifyChannel;

    @Mock
    private AdminAlerter adminAlerter;

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
        lenient().when(applications.findPendingByUsername(anyString())).thenReturn(Optional.empty());
        lenient().when(applications.findPendingByPhone(anyString())).thenReturn(Optional.empty());
        lenient().when(applications.countPending()).thenReturn(0L);
        lenient().when(passwordHasher.encode(anyString())).thenReturn("hash");
        // 昵称校验逻辑在 AppUserService（本体是 AccountRules），这里按真实规则打桩（注册必填）
        lenient().when(userService.requireValidNickname(any()))
                .thenAnswer(inv -> {
                    String nickname = inv.getArgument(0);
                    String trimmed = nickname == null ? "" : nickname.trim();
                    if (trimmed.isEmpty()) {
                        throw new BusinessException(400, "请输入昵称（1~32 个字）");
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
    void applyStoresRequiredNickname() {
        RegistrationService.ApplicationVO vo = apply("张三");

        ArgumentCaptor<RegistrationApplication> captor =
                ArgumentCaptor.forClass(RegistrationApplication.class);
        verify(applications).save(captor.capture());
        assertThat(captor.getValue().nickname()).isEqualTo("张三");
        assertThat(vo.nickname()).isEqualTo("张三");
        assertThat(vo.status()).isEqualTo(RegistrationApplication.STATUS_PENDING);
    }

    @Test
    void applyRejectsMissingNickname() {
        assertThatThrownBy(() -> apply(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请输入昵称");
        assertThatThrownBy(() -> apply("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请输入昵称");
        verify(applications, never()).save(any(RegistrationApplication.class));
    }

    @Test
    void applyRejectsTooLongNickname() {
        assertThatThrownBy(() -> apply("x".repeat(33)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称");
        verify(applications, never()).save(any(RegistrationApplication.class));
    }

    @Test
    void applyNotifiesAdminsWithPendingCount() {
        when(applications.countPending()).thenReturn(7L);

        apply("张三");

        verify(notifyChannel).pushTextAsync(anyString(), anyString());
        verify(adminAlerter).publishPendingCount(anyList(), anyLong());
    }

    @Test
    void applyMasksPhoneInAdminPush() {
        // 站外推送只给脱敏手机号（139****1111），期望值与改造前的 mask() 结果一致
        apply("张三");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifyChannel).pushTextAsync(anyString(), body.capture());
        assertThat(body.getValue()).contains("139****1111");
        assertThat(body.getValue()).doesNotContain("13900001111");
    }

    @Test
    void duplicatePendingUsernameIsRejectedBeforeSaving() {
        when(applications.findPendingByUsername("zhangsan"))
                .thenReturn(Optional.of(RegistrationApplication.submit("13900001111", "zhangsan", "张三", "hash")));

        assertThatThrownBy(() -> apply("张三"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该用户名已有待审批的申请，请耐心等待管理员处理");
        verify(applications, never()).save(any(RegistrationApplication.class));
    }
}
