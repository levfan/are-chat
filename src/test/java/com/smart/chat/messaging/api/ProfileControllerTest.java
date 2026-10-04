package com.smart.chat.messaging.api;

import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.bootstrap.config.FastJsonWebConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 个人资料：登录用户修改昵称（user_profile + app_user 同步）、签名等字段。
 * <p>接线从 Mapper 换成仓储端口；期望值（JSON 字段、文案、条数与排序）与改造前一字不差。
 */
@WebMvcTest(ProfileController.class)
@Import(FastJsonWebConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileRepository profileRepository;

    @MockitoBean
    private FriendRepository friendRepository;

    @MockitoBean
    private AccountDirectory accounts;

    private static UserProfile profileOf(String username) {
        return UserProfile.restore(username, username, "", "c0", "online", null, 1L);
    }

    @Test
    void updateNicknameSyncsAppUser() throws Exception {
        when(profileRepository.find("alice")).thenReturn(Optional.of(profileOf("alice")));

        mockMvc.perform(put("/api/profile")
                        .sessionAttr("CurrentUser", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\",\"signature\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"));

        verify(accounts).updateNickname("alice", "新昵称");
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().nickname()).isEqualTo("新昵称");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().signature()).isEqualTo("你好");
    }

    @Test
    void updateNicknameRejectsTooLongAndSkipsSync() throws Exception {
        when(profileRepository.find("alice")).thenReturn(Optional.of(profileOf("alice")));

        mockMvc.perform(put("/api/profile")
                        .sessionAttr("CurrentUser", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "x".repeat(33) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("昵称需为 1~32 个字"));

        verify(accounts, never()).updateNickname(eq("alice"), anyString());
    }

    private static Friend friendOf(String owner, String peer) {
        return Friend.restore(owner + "-" + peer, owner, peer, "", null, false, false, null, 0L, null, 1L);
    }

    @Test
    void friendsBirthdaysReadsAllProfilesInOneBatch() throws Exception {
        when(friendRepository.findAllByOwner("alice"))
                .thenReturn(List.of(friendOf("alice", "bob"), friendOf("alice", "carol")));
        UserProfile bob = profileOf("bob");
        bob.changeNickname("阿波");
        bob.changeBirthday("1990-" + LocalDate.now().minusDays(1).format(
                java.time.format.DateTimeFormatter.ofPattern("MM-dd")));
        UserProfile carol = profileOf("carol");
        carol.changeBirthday(LocalDate.now().toString());
        when(profileRepository.listByUsernames(List.of("bob", "carol"))).thenReturn(List.of(carol, bob));

        mockMvc.perform(get("/api/profile/friends-birthdays").sessionAttr("CurrentUser", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                // 今天生日的排最前，昨天生日的排到最后（按今年剩余天数升序）
                .andExpect(jsonPath("$.data[0].username").value("carol"))
                .andExpect(jsonPath("$.data[0].today").value(true))
                .andExpect(jsonPath("$.data[0].daysUntil").value(0))
                .andExpect(jsonPath("$.data[1].username").value("bob"))
                .andExpect(jsonPath("$.data[1].nickname").value("阿波"))
                // 不断言具体天数：跨年那天的剩余天数随闰年变化（364/365/366），只锁「昨天的生日排到最后」
                .andExpect(jsonPath("$.data[1].today").value(false));

        // 好友生日表一次批量取资料：原先每个好友 selectById 一次（N+1）
        verify(profileRepository).listByUsernames(List.of("bob", "carol"));
        verify(profileRepository, never()).find(anyString());
    }

    @Test
    void friendsBirthdaysSkipsProfileLookupWithoutFriends() throws Exception {
        when(friendRepository.findAllByOwner("alice")).thenReturn(List.of());

        mockMvc.perform(get("/api/profile/friends-birthdays").sessionAttr("CurrentUser", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 没好友时一个资料查询都不发（批量取传空集合会拼出 IN ()）
        verify(profileRepository, never()).listByUsernames(any());
        verify(profileRepository, never()).find(anyString());
    }
}
