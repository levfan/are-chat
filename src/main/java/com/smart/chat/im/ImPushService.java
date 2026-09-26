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

    /** 84 会话内置顶消息：推给会话双方（pinned=false 为取消置顶）。 */
    public record PinPayload(String type, String peerA, String peerB, String msgId, boolean pinned) {
    }

    /** 88 全站公告：推给所有在线用户。 */
    public record AnnouncementPayload(String type, String announcementId, String content) {
    }

    /** 78 管理员待办：新注册申请提醒（只推给在线管理员）。 */
    public record AdminEventPayload(String type, long pendingCount) {
    }

    /** 情侣空间事件：邀请/同意/约定打卡/早晚安/今日一问/共享清单等（username 为动作发起方，接收方视角即「TA」）。 */
    public record CoupleEventPayload(String type, String event, String username, String detail) {
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

    /** 推给全部在线用户（88 公告广播） */
    public void pushAll(Object payload) {
        String json = JSON.toJSONString(payload);
        for (String user : registry.onlineUsers()) {
            registry.sendToUser(user, json);
        }
    }

    /** 推给一组用户（78 在线管理员） */
    public void pushToUsers(Iterable<String> usernames, Object payload) {
        String json = JSON.toJSONString(payload);
        for (String user : usernames) {
            registry.sendToUser(user, json);
        }
    }

    /** 78 在线管理员收到新注册申请待办 */
    public void pushAdminEvent(Iterable<String> adminUsernames, long pendingCount) {
        AdminEventPayload payload = new AdminEventPayload("admin-pending", pendingCount);
        for (String admin : adminUsernames) {
            push(admin, payload);
        }
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

    /** 84 置顶/取消置顶：推给会话双方。 */
    public void pushPin(String peerA, String peerB, String msgId, boolean pinned) {
        PinPayload payload = new PinPayload("pin", peerA, peerB, msgId, pinned);
        push(peerA, payload);
        if (!peerA.equals(peerB)) {
            push(peerB, payload);
        }
    }

    /** 情侣空间事件：推给除发起方之外的接收人（username 记录发起方）。 */
    public void pushCoupleEvent(String event, String actor, String toUser, String detail) {
        push(toUser, new CoupleEventPayload("couple", event, actor, detail));
    }

    /** 情侣空间事件：推给双方（如今日仪式解锁），actor 为触发者。 */
    public void pushCoupleEventBoth(String event, String actor, String userA, String userB, String detail) {
        CoupleEventPayload payload = new CoupleEventPayload("couple", event, actor, detail);
        push(userA, payload);
        if (!userA.equals(userB)) {
            push(userB, payload);
        }
    }
}
