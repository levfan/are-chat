package com.smart.chat.im;

import com.alibaba.fastjson2.JSON;
import com.smart.chat.room.ChatSessionRegistry;
import org.springframework.stereotype.Component;

/**
 * IM 推送服务：把私聊/输入状态/撤回/好友事件通过 WebSocket 推给在线用户。
 * 复用聊天室的连接注册表（同一端点允许多端在线）。
 */
@Component
public class ImPushService {

    public record DmPayload(String type, String msgId, String from, String to,
                            String content, String msgType, String replyToId, Boolean edited, Long created) {
        public static DmPayload of(PrivateMessage m) {
            return new DmPayload("dm", m.getId(), m.getFromUser(), m.getToUser(),
                    m.getContent(), m.getMsgType(), m.getReplyToId(),
                    Integer.valueOf(1).equals(m.getEdited()), m.getCreated());
        }
    }

    public record TypingPayload(String type, String from, String to, Boolean typing) {
    }

    public record RecallPayload(String type, String from, String to, String msgId) {
    }

    public record FriendEventPayload(String type, String username, String requestId) {
    }

    /** 消息表情回应：推给会话双方。 */
    public record ReactionPayload(String type, String msgId, String by, String emoji, boolean added, String from, String to) {
    }

    /** 消息编辑：推给会话双方。 */
    public record EditPayload(String type, String msgId, String from, String to, String content) {
    }

    /** 已读回执：reader 读完了与 peer 的会话，推给 peer。 */
    public record ReadPayload(String type, String reader, String peer) {
    }

    private final ChatSessionRegistry registry;

    public ImPushService(ChatSessionRegistry registry) {
        this.registry = registry;
    }

    public boolean isOnline(String username) {
        return registry.onlineUsers().contains(username);
    }

    public void push(String username, Object payload) {
        registry.sendToUser(username, JSON.toJSONString(payload));
    }

    public void pushDm(PrivateMessage message) {
        push(message.getToUser(), DmPayload.of(message));
    }

    public void pushTyping(String from, String to, boolean typing) {
        push(to, new TypingPayload("typing", from, to, typing));
    }

    public void pushRecall(PrivateMessage message) {
        push(message.getToUser(), new RecallPayload("recall", message.getFromUser(), message.getToUser(), message.getId()));
    }

    public void pushFriendEvent(String type, String username, String requestId) {
        push(username, new FriendEventPayload(type, username, requestId));
    }

    /** 回应推给会话双方（发起方也收一份，多端同步）。 */
    public void pushReaction(PrivateMessage message, String by, String emoji, boolean added) {
        ReactionPayload payload = new ReactionPayload("reaction", message.getId(), by, emoji, added,
                message.getFromUser(), message.getToUser());
        push(message.getFromUser(), payload);
        if (!message.getFromUser().equals(message.getToUser())) {
            push(message.getToUser(), payload);
        }
    }

    public void pushEdit(PrivateMessage message) {
        EditPayload payload = new EditPayload("message-edit", message.getId(),
                message.getFromUser(), message.getToUser(), message.getContent());
        push(message.getFromUser(), payload);
        push(message.getToUser(), payload);
    }

    /** reader 已读完与 peer 的会话：把回执推给 peer。 */
    public void pushRead(String reader, String peer) {
        if (isOnline(peer)) {
            push(peer, new ReadPayload("read", reader, peer));
        }
    }
}
