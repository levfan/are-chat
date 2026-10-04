package com.smart.chat.messaging.api;

import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.messaging.infrastructure.transport.ChatSessionRegistry;
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
 * <p>好友名单改从 {@link FriendRepository} 端口取（原来直接摸 FriendMapper）。
 */
@ExtendWith(MockitoExtension.class)
class PresenceControllerTest {

    @Mock
    private ChatSessionRegistry registry;

    @Mock
    private AccountDirectory accounts;

    @Mock
    private FriendRepository friendRepository;

    @InjectMocks
    private PresenceController controller;

    private static HttpSession sessionOf(String username) {
        HttpSession session = mock(HttpSession.class);
        lenient().when(session.getAttribute("CurrentUser")).thenReturn(username);
        return session;
    }

    private static Friend edge(String owner, String peer) {
        return Friend.add(owner, peer, 1L);
    }

    @Test
    void adminSeesWholeSiteOnlineUsers() {
        when(registry.onlineUsers()).thenReturn(Set.of("alice", "bob", "admin"));
        lenient().when(accounts.find("admin")).thenReturn(Optional.of(
                new AccountDirectory.Account("admin", "管理员", true, "138****0000")));

        ApiResponse<PresenceController.OnlineVO> response = controller.online(sessionOf("admin"));

        assertThat(response.data().onlineCount()).isEqualTo(3);
        assertThat(response.data().users()).containsExactlyInAnyOrder("alice", "bob", "admin");
        // 管理员不需要查好友表
        verify(friendRepository, never()).findAllByOwner("admin");
    }

    @Test
    void regularUserOnlyCountsOnlineFriends() {
        when(registry.onlineUsers()).thenReturn(Set.of("alice", "bob", "carol"));
        when(accounts.find("alice")).thenReturn(Optional.of(
                new AccountDirectory.Account("alice", "alice", false, "138****0001")));
        // alice 的通讯录：bob 与 dave（carol 不是好友）
        when(friendRepository.findAllByOwner("alice"))
                .thenReturn(List.of(edge("alice", "bob"), edge("alice", "dave")));

        ApiResponse<PresenceController.OnlineVO> response = controller.online(sessionOf("alice"));

        assertThat(response.data().onlineCount()).isEqualTo(1);
        assertThat(response.data().users()).containsExactly("bob");
    }

    @Test
    void regularUserWithoutFriendsSeesZero() {
        when(registry.onlineUsers()).thenReturn(Set.of("alice", "bob"));
        when(accounts.find("alice")).thenReturn(Optional.of(
                new AccountDirectory.Account("alice", "alice", false, "138****0001")));
        when(friendRepository.findAllByOwner("alice")).thenReturn(List.of());

        ApiResponse<PresenceController.OnlineVO> response = controller.online(sessionOf("alice"));

        assertThat(response.data().onlineCount()).isZero();
        assertThat(response.data().users()).isEmpty();
    }
}
