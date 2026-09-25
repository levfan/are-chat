package com.smart.chat.room;

import com.smart.chat.im.PresenceService;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketBridgeTest {

    @Mock
    private ChatSessionRegistry registry;

    @Mock
    private PresenceService presenceService;

    @Mock
    private Session selfSession;

    @Mock
    private RemoteEndpoint.Basic selfBasic;

    private ChatSessionRegistry.Connection self;
    private ChatWebSocketBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new ChatWebSocketBridge(registry, presenceService);
        self = new ChatSessionRegistry.Connection("conn-1", "小A", selfSession);
        lenient().when(selfSession.getBasicRemote()).thenReturn(selfBasic);
    }

    @Test
    void jsonHeartbeatGetsAck() throws Exception {
        bridge.onMessage(self, "{\"type\":\"heart\"}");

        verify(selfBasic).sendText(contains("\"type\":\"heart-ack\""));
    }

    @Test
    void legacyHeartBeatStringStillWorks() throws Exception {
        bridge.onMessage(self, "heartBeat-2024");

        verify(selfBasic).sendText(contains("heart-ack"));
    }

    @Test
    void legacyChatTypeIsRejected() throws Exception {
        bridge.onMessage(self, "{\"type\":\"chat\",\"name\":\"小A\",\"subject\":\"小B\",\"msg\":\"在吗\"}");

        verify(selfBasic).sendText(contains("服务端消息不允许客户端伪造"));
    }

    @Test
    void malformedJsonGetsSystemReply() throws Exception {
        bridge.onMessage(self, "这不是JSON");

        verify(selfBasic).sendText(contains("消息格式错误"));
    }

    @Test
    void unknownTypeGetsSystemReply() throws Exception {
        bridge.onMessage(self, "{\"type\":\"dance\"}");

        verify(selfBasic).sendText(contains("未知的消息类型"));
    }

    @Test
    void onOpenRegistersAndWelcomes() throws Exception {
        Session session = org.mockito.Mockito.mock(Session.class);
        RemoteEndpoint.Basic basic = org.mockito.Mockito.mock(RemoteEndpoint.Basic.class);
        when(session.getBasicRemote()).thenReturn(basic);
        when(registry.onlineUsers()).thenReturn(java.util.Set.of());

        bridge.onOpen(session, "dave");

        verify(registry).register(eq("dave"), any(ChatSessionRegistry.Connection.class));
        verify(basic).sendText(contains("欢迎来到 are-chat，dave！"));
        verify(presenceService).touch("dave");
    }

    @Test
    void onCloseRemovesAndBroadcasts() {
        when(registry.onlineUsers()).thenReturn(java.util.Set.of());

        bridge.onClose(self);

        verify(registry).remove(eq("小A"), eq("conn-1"));
        verify(presenceService).touch("小A");
    }

    @Test
    void typingIsForwardedWithConnectionName() {
        when(registry.sendToUser(eq("小B"), contains("\"type\":\"typing\""))).thenReturn(1);

        bridge.onMessage(self, "{\"type\":\"typing\",\"name\":\"伪造成别人\",\"subject\":\"小B\",\"msg\":\"1\"}");

        // from 必须是连接昵称「小A」，不能信任客户端字段
        verify(registry).sendToUser(eq("小B"), argThat(arg ->
                arg.contains("\"type\":\"typing\"") && arg.contains("\"name\":\"小A\"")));
    }

    @Test
    void serverOnlyTypesCannotBeForged() throws Exception {
        bridge.onMessage(self, "{\"type\":\"dm\",\"from\":\"小A\",\"to\":\"小B\",\"content\":\"hi\"}");

        verify(selfBasic).sendText(contains("服务端消息不允许客户端伪造"));
    }
}
