package com.smart.chat.room;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionRegistryTest {

    @Mock
    private Session sessionA1;

    @Mock
    private Session sessionA2;

    @Mock
    private Session sessionB1;

    private void stub(Session session) throws IOException {
        RemoteEndpoint.Basic basic = org.mockito.Mockito.mock(RemoteEndpoint.Basic.class);
        lenient().when(session.getBasicRemote()).thenReturn(basic);
    }

    @Test
    void sameUserCanHaveMultipleConnections() throws IOException {
        stub(sessionA1);
        stub(sessionA2);
        ChatSessionRegistry registry = new ChatSessionRegistry();

        assertThat(registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA1))).isTrue();
        assertThat(registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA2))).isTrue();

        assertThat(registry.connectionCount()).isEqualTo(2);
        assertThat(registry.onlineUsers()).containsExactly("A");
    }

    @Test
    void duplicateConnectionIdIsRejected() throws IOException {
        stub(sessionA1);
        ChatSessionRegistry registry = new ChatSessionRegistry();
        ChatSessionRegistry.Connection connection = ChatSessionRegistry.Connection.of("A", sessionA1);

        assertThat(registry.register("A", connection)).isTrue();
        assertThat(registry.register("A", connection)).isFalse();
    }

    @Test
    void sendToUserDeliversToAllConnectionsOfUser() throws IOException {
        stub(sessionA1);
        stub(sessionA2);
        stub(sessionB1);
        ChatSessionRegistry registry = new ChatSessionRegistry();
        registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA1));
        registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA2));
        registry.register("B", ChatSessionRegistry.Connection.of("B", sessionB1));

        int delivered = registry.sendToUser("A", "hello-A");

        assertThat(delivered).isEqualTo(2);
    }

    @Test
    void sendToUnknownUserDeliversNothing() throws IOException {
        stub(sessionA1);
        ChatSessionRegistry registry = new ChatSessionRegistry();
        registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA1));

        assertThat(registry.sendToUser("Ghost", "hi")).isZero();
    }

    @Test
    void sendFailureStillCountsOthers() throws IOException {
        stub(sessionA1);
        stub(sessionA2);
        ChatSessionRegistry registry = new ChatSessionRegistry();
        registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA1));
        registry.register("A", ChatSessionRegistry.Connection.of("A", sessionA2));
        // 让第一个连接发送失败（先取出嵌套 mock，再对其打桩，避免 UnfinishedStubbing）
        RemoteEndpoint.Basic brokenBasic = sessionA1.getBasicRemote();
        org.mockito.Mockito.doThrow(new IOException("broken")).when(brokenBasic).sendText("msg");

        int delivered = registry.sendToUser("A", "msg");

        assertThat(delivered).isEqualTo(1);
    }

    @Test
    void removeCleansUpAndKeepsOtherUsers() throws IOException {
        stub(sessionA1);
        stub(sessionB1);
        ChatSessionRegistry registry = new ChatSessionRegistry();
        ChatSessionRegistry.Connection a = ChatSessionRegistry.Connection.of("A", sessionA1);
        registry.register("A", a);
        registry.register("B", ChatSessionRegistry.Connection.of("B", sessionB1));

        registry.remove("A", a.id());

        assertThat(registry.connectionCount()).isEqualTo(1);
        assertThat(registry.onlineUsers()).isEqualTo(Set.of("B"));
    }

    @Test
    void sendToBrokenSessionReturnsFalse() throws IOException {
        stub(sessionA1);
        ChatSessionRegistry registry = new ChatSessionRegistry();
        RemoteEndpoint.Basic brokenBasic = sessionA1.getBasicRemote();
        org.mockito.Mockito.doThrow(new IOException("broken")).when(brokenBasic).sendText("x");

        assertThat(ChatSessionRegistry.sendToSession(sessionA1, "x")).isFalse();
    }
}
