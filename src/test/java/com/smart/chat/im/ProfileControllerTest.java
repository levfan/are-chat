package com.smart.chat.im;

import com.smart.chat.auth.AppUserService;
import com.smart.chat.config.FastJsonWebConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 个人资料：登录用户修改昵称（user_profile + app_user 同步）、签名等字段 */
@WebMvcTest(ProfileController.class)
@Import(FastJsonWebConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileMapper profileMapper;

    @MockitoBean
    private FriendMapper friendMapper;

    @MockitoBean
    private AppUserService userService;

    private UserProfile profileOf(String username) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setNickname(username);
        profile.setSignature("");
        profile.setAvatar("c0");
        profile.setPresenceStatus("online");
        profile.setUpdatedAt(1L);
        return profile;
    }

    @Test
    void updateNicknameSyncsAppUser() throws Exception {
        when(profileMapper.selectById("alice")).thenReturn(profileOf("alice"));

        mockMvc.perform(put("/api/profile")
                        .sessionAttr("CurrentUser", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\",\"signature\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"));

        verify(userService).updateNickname("alice", "新昵称");
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).updateById(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getNickname()).isEqualTo("新昵称");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getSignature()).isEqualTo("你好");
    }

    @Test
    void updateNicknameRejectsTooLongAndSkipsSync() throws Exception {
        when(profileMapper.selectById("alice")).thenReturn(profileOf("alice"));

        mockMvc.perform(put("/api/profile")
                        .sessionAttr("CurrentUser", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "x".repeat(33) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("昵称需为 1~32 个字"));

        verify(userService, never()).updateNickname(eq("alice"), org.mockito.ArgumentMatchers.anyString());
    }
}
