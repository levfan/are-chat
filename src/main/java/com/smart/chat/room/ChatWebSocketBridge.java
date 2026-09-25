package com.smart.chat.room;

import com.alibaba.fastjson2.JSON;
import com.smart.chat.im.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * IM WebSocket 协议桥：/ws/chat/{昵称} 是 IM 的实时推送通道。
 * - 心跳：{"type":"heart"} 或旧版裸字符串 "heartBeat"
 * - typing：对方正在输入转发（发送者以连接昵称为准）
 * - presence：上下线广播（好友列表绿点）
 * - dm/recall/friend-* 等服务端事件只由服务端发出，客户端伪造一律拒绝
 */
@Component
public class ChatWebSocketBridge {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketBridge.class);

    private final ChatSessionRegistry registry;
    private final PresenceService presenceService;

    public ChatWebSocketBridge(ChatSessionRegistry registry, PresenceService presenceService) {
        this.registry = registry;
        this.presenceService = presenceService;
        // @ServerEndpoint 实例由 WS 容器创建，无法走 Spring 注入，这里桥接到静态引用。
        ChatEndpoint.bridge = this;
    }

    public ChatSessionRegistry.Connection onOpen(jakarta.websocket.Session session, String rawName) {
        String name = decode(rawName);
        if (name.isBlank()) {
            name = "游客" + session.getId().substring(0, Math.min(6, session.getId().length()));
        }
        ChatSessionRegistry.Connection connection = ChatSessionRegistry.Connection.of(name, session);
        registry.register(name, connection);
        broadcastPresence(name, true);
        presenceService.touch(name);
        ChatSessionRegistry.sendToSession(session,
                ChatMessage.system(name, "欢迎来到 are-chat，" + name + "！").toJson());
        log.info("有新连接加入！昵称：{}，当前连接数：{}", name, registry.connectionCount());
        return connection;
    }

    public void onClose(ChatSessionRegistry.Connection connection) {
        if (connection == null) {
            return;
        }
        registry.remove(connection.username(), connection.id());
        broadcastPresence(connection.username(), false);
        presenceService.touch(connection.username());
        log.info("有一连接关闭！当前连接数：{}", registry.connectionCount());
    }

    public void onMessage(ChatSessionRegistry.Connection self, String raw) {
        if (self == null || raw == null || raw.isBlank()) {
            return;
        }
        // 兼容老客户端的心跳裸字符串
        if (raw.contains("heartBeat")) {
            ChatSessionRegistry.sendToSession(self.session(), ChatMessage.heartAck().toJson());
            return;
        }
        ChatMessage msg;
        try {
            msg = JSON.parseObject(raw, ChatMessage.class);
        } catch (Exception e) {
            ChatSessionRegistry.sendToSession(self.session(),
                    ChatMessage.system(self.username(), "消息格式错误，需要 JSON：{type,subject,msg}").toJson());
            return;
        }
        if (msg == null) {
            return;
        }
        String type = msg.type() == null ? "" : msg.type();
        switch (type) {
            case "heart" -> ChatSessionRegistry.sendToSession(self.session(), ChatMessage.heartAck().toJson());
            case "typing" -> handleTyping(self, msg);
            case "presence", "dm", "recall", "friend-request", "friend-accepted", "friend-deleted", "chat" ->
                    ChatSessionRegistry.sendToSession(self.session(),
                            ChatMessage.system(self.username(), "服务端消息不允许客户端伪造：" + type).toJson());
            default -> ChatSessionRegistry.sendToSession(self.session(),
                    ChatMessage.system(self.username(), "未知的消息类型：" + type).toJson());
        }
    }

    /** IM「对方正在输入」：from 以连接昵称为准，不信任客户端字段。 */
    private void handleTyping(ChatSessionRegistry.Connection self, ChatMessage msg) {
        if (isBlank(msg.subject())) {
            return;
        }
        boolean typing = !"0".equals(msg.msg());
        registry.sendToUser(msg.subject(), ChatMessage.typing(self.username(), msg.subject(), typing).toJson());
    }

    /** IM 在线状态广播：好友列表绿点用。 */
    private void broadcastPresence(String username, boolean online) {
        String json = ChatMessage.presence(username, online).toJson();
        for (String user : registry.onlineUsers()) {
            registry.sendToUser(user, json);
        }
    }

    private String decode(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return raw.trim();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
