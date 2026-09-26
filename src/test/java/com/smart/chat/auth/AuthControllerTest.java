package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.config.FastJsonWebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 账号体系：手机号注册 + 手机号/用户名登录（业务规则在 AppUserService，这里只验接口契约） */
@WebMvcTest(AuthController.class)
@Import({FastJsonWebConfig.class, LoginRateLimiter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService userService;

    @MockitoBean
    private SmsCodeService smsCodeService;

    @MockitoBean
    private RegistrationService registrationService;

    private AppUser alice() {
        return AppUser.of("13800000001", "alice", "hash", "alice", "c0");
    }

    @BeforeEach
    void stubNormalize() {
        lenient().when(userService.normalizeAccount(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).trim().toLowerCase());
    }

    @Test
    void loginWithAccountAndPasswordStoresSession() throws Exception {
        when(userService.login("alice", "arechat123")).thenReturn(alice());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"alice\",\"password\":\"arechat123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.phone").value("138****0001"))
                .andExpect(request().sessionAttribute("CurrentUser", "alice"));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        when(userService.login("alice", "bad-pass1"))
                .thenThrow(new BusinessException(401, "账号或密码不正确"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"alice\",\"password\":\"bad-pass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("账号或密码不正确"));
    }

    @Test
    void loginWithBlankAccountIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"  \",\"password\":\"arechat123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入手机号或用户名"));
    }

    @Test
    void smsCodeReturnsDevCodeForDemo() throws Exception {
        when(userService.requireValidPhone("13900001111")).thenReturn("13900001111");
        when(smsCodeService.issue("13900001111")).thenReturn("123456");
        when(smsCodeService.ttlSeconds()).thenReturn(300L);

        mockMvc.perform(post("/api/auth/sms-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900001111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.devCode").value("123456"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));
    }

    @Test
    void registerSubmitsApprovalApplicationInsteadOfLogin() throws Exception {
        // 77 注册不再直接建号登录：只提交待审批申请（昵称选填，随申请一起传给服务层）
        when(registrationService.apply("13900001111", "zhangsan", "abc12345", "123456", "张三"))
                .thenReturn(new RegistrationService.ApplicationVO("app-1", "zhangsan", "张三", "139****1111",
                        RegistrationApplication.STATUS_PENDING, null, 1L, null, null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900001111\",\"username\":\"zhangsan\",\"nickname\":\"张三\","
                                + "\"password\":\"abc12345\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("zhangsan"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(request().sessionAttributeDoesNotExist("CurrentUser"));
    }

    @Test
    void registerStatusExposesApplicationState() throws Exception {
        when(registrationService.statusByAccount("zhangsan"))
                .thenReturn(java.util.Optional.of(new RegistrationService.ApplicationVO("app-1", "zhangsan", "张三",
                        "139****1111", RegistrationApplication.STATUS_REJECTED, "资料不全", 1L, 2L, "admin")));

        mockMvc.perform(get("/api/auth/register-status").param("account", "zhangsan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value("资料不全"));
    }

    @Test
    void changePasswordDelegatesToService() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/auth/password")
                        .sessionAttr("CurrentUser", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"abc12345\",\"newPassword\":\"xw922ghk\"}"))
                .andExpect(status().isOk());

        verify(userService).changePassword("alice", "abc12345", "xw922ghk");
    }

    @Test
    void meWithoutSessionIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void meWithSessionReturnsUser() throws Exception {
        when(userService.find("bob")).thenReturn(Optional.of(
                AppUser.of("13800000002", "bob", "hash", "bob", "c1")));

        mockMvc.perform(get("/api/auth/me").sessionAttr("CurrentUser", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("bob"));
    }
}
