package com.smart.chat.room;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 聊天室端点：/ws/chat/{昵称}（昵称需 URL 编码）。
 * 实例由 WS 容器按连接创建，业务逻辑全部委托给 ChatWebSocketBridge。
 * 标注 @Component 仅为了让 ServerEndpointExporter 在容器中发现并注册该端点类。
 */
@Component
@ServerEndpoint("/ws/chat/{name}")
public class ChatEndpoint {

    private static final Logger log = LoggerFactory.getLogger(ChatEndpoint.class);

    static volatile ChatWebSocketBridge bridge;

    private ChatSessionRegistry.Connection connection;

    @OnOpen
    public void onOpen(Session session, @PathParam("name") String name) {
        this.connection = bridge().onOpen(session, name);
    }

    @OnMessage
    public void onMessage(String message) {
        if (connection == null) {
            return;
        }
        bridge().onMessage(connection, message);
    }

    @OnClose
    public void onClose() {
        if (connection == null) {
            return;
        }
        bridge().onClose(connection);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        // 连接异常（多为客户端直接断开）只记录，不影响其他连接
        log.warn("聊天连接异常：{}", error.getMessage());
    }

    private static ChatWebSocketBridge bridge() {
        ChatWebSocketBridge b = bridge;
        if (b == null) {
            throw new IllegalStateException("ChatWebSocketBridge 尚未初始化");
        }
        return b;
    }
}
