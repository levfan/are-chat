package com.smart.chat.messaging.api;

import com.smart.chat.messaging.infrastructure.persistence.FriendPO;
import com.smart.chat.messaging.infrastructure.persistence.FriendMapper;
import com.smart.chat.messaging.infrastructure.persistence.UserProfilePO;
import com.smart.chat.messaging.infrastructure.persistence.UserProfileMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private AccountDirectory accounts;

    private UserProfilePO profileOf(String username) {
        UserProfilePO profile = new UserProfilePO();
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

        verify(accounts).updateNickname("alice", "新昵称");
        ArgumentCaptor<UserProfilePO> captor = ArgumentCaptor.forClass(UserProfilePO.class);
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

        verify(accounts, never()).updateNickname(eq("alice"), org.mockito.ArgumentMatchers.anyString());
    }

    private FriendPO friendOf(String owner, String peer) {
        FriendPO friend = new FriendPO();
        friend.setId(owner + "-" + peer);
        friend.setOwnerUsername(owner);
        friend.setFriendUsername(peer);
        return friend;
    }

    @Test
    void friendsBirthdaysReadsAllProfilesInOneBatch() throws Exception {
        when(friendMapper.findAllByOwner("alice")).thenReturn(List.of(friendOf("alice", "bob"), friendOf("alice", "carol")));
        UserProfilePO bob = profileOf("bob");
        bob.setNickname("阿波");
        bob.setBirthday("1990-" + LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")));
        UserProfilePO carol = profileOf("carol");
        carol.setBirthday(LocalDate.now().toString());
        when(profileMapper.selectBatchIds(List.of("bob", "carol"))).thenReturn(List.of(carol, bob));

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
        verify(profileMapper).selectBatchIds(List.of("bob", "carol"));
        verify(profileMapper, never()).selectById(any(java.io.Serializable.class));
    }

    @Test
    void friendsBirthdaysSkipsProfileLookupWithoutFriends() throws Exception {
        when(friendMapper.findAllByOwner("alice")).thenReturn(List.of());

        mockMvc.perform(get("/api/profile/friends-birthdays").sessionAttr("CurrentUser", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 没好友时一个资料查询都不发（selectBatchIds 传空集合会拼出 IN ()）
        verify(profileMapper, never()).selectBatchIds(any());
        verify(profileMapper, never()).selectById(any(java.io.Serializable.class));
    }
}
