package com.smart.chat.im;

import com.smart.chat.auth.AppUser;
import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.ApiResponse;
import com.smart.chat.room.ChatSessionRegistry;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 57 在线人数按角色区分：管理员统计全站所有在线用户，普通用户只统计通讯录好友里的在线人数。
 */
@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock
    private ChatSessionRegistry registry;

    @Mock
    private AppUserService userService;

    @Mock
    private FriendMapper friendMapper;

    @InjectMocks
    private PresenceController controller;

    private static HttpSession sessionOf(String username) {
        HttpSession session = mock(HttpSession.class);
        lenient().when(session.getAttribute("CurrentUser")).thenReturn(username);
        return session;
    }

    @Test
    void adminSeesWholeSiteOnlineUsers() {
        when(registry.onlineUsers()).thenReturn(Set.of("alice", "bob", "admin"));
        lenient().when(userService.find("admin")).thenReturn(Optional.of(
                AppUser.of("13800000000", "admin", "hash", "管理员", "c0", AppUser.ROLE_ADMIN)));

        ApiResponse<PresenceController.OnlineVO> response = controller.online(sessionOf("admin"));

        assertThat(response.data().onlineCount()).isEqualTo(3);
        assertThat(response.data().users()).containsExactlyInAnyOrder("alice", "bob", "admin");
        // 管理员不需要查好友表
        verify(friendMapper, never()).findAllByOwner("admin");
    }

    @Test
    void regularUserOnlyCountsOnlineFriends() {
        when(registry.onlineUsers()).thenReturn(Set.of("alice", "bob", "carol"));
        when(userService.find("alice")).thenReturn(Optional.of(
                AppUser.of("13800000001", "alice", "hash", "alice", "c0")));
        // alice 的通讯录：bob 与 dave（carol 不是好友）
        when(friendMapper.findAllByOwner("alice"))
                .thenReturn(List.of(Friend.of("alice", "bob"), Friend.of("alice", "dave")));

        ApiResponse<PresenceController.OnlineVO> response = controller.online(sessionOf("alice"));

        assertThat(response.data().onlineCount()).isEqualTo(1);
        assertThat(response.data().users()).containsExactly("bob");
    }

    @Test
    void regularUserWithoutFriendsSeesZero() {
        when(registry.onlineUsers()).thenReturn(Set.of("alice", "bob"));
        when(userService.find("alice")).thenReturn(Optional.of(
                AppUser.of("13800000001", "alice", "hash", "alice", "c0")));
        when(friendMapper.findAllByOwner("alice")).thenReturn(List.of());

        ApiResponse<PresenceController.OnlineVO> response = controller.online(sessionOf("alice"));

        assertThat(response.data().onlineCount()).isZero();
        assertThat(response.data().users()).isEmpty();
    }
}
