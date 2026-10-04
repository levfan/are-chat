package com.smart.chat.messaging.domain.pin;

import java.util.UUID;

/**
 * 84 会话内置顶消息：一个会话最多一条，<b>双方可见</b>，任何一方置顶都覆盖前一条。
 * <p>
 * 「最多一条」是这条聚合的唯一不变式，落在 {@link Conversation} 的规范化与
 * {@link ConversationPinRepository#replace} 的先删后插上；「谁置顶的」记在 {@code createdBy}，
 * 对外投影要回显它。除此之外没有状态迁移，所以类型保持薄。
 */
public final class ConversationPin {

    private final String id;
    private final String userA;
    private final String userB;
    private final String msgId;
    private final String createdBy;
    private final Long created;

    private ConversationPin(String id, String userA, String userB, String msgId, String createdBy, Long created) {
        this.id = id;
        this.userA = userA;
        this.userB = userB;
        this.msgId = msgId;
        this.createdBy = createdBy;
        this.created = created;
    }

    /** 由发起置顶的人与对方，规范化地建一条置顶记录。 */
    public static ConversationPin by(String me, String peer, String msgId, long at) {
        Conversation conversation = Conversation.between(me, peer);
        return new ConversationPin(UUID.randomUUID().toString(), conversation.userA(), conversation.userB(),
                msgId, me, at);
    }

    /** 从存储重建：不校验。 */
    public static ConversationPin restore(String id, String userA, String userB, String msgId, String createdBy,
                                          Long created) {
        return new ConversationPin(id, userA, userB, msgId, createdBy, created);
    }

    /** 这条置顶属于哪个会话（存储侧的唯一键）。 */
    public Conversation conversation() {
        return Conversation.between(userA, userB);
    }

    public String id() {
        return id;
    }

    public String userA() {
        return userA;
    }

    public String userB() {
        return userB;
    }

    public String msgId() {
        return msgId;
    }

    public String createdBy() {
        return createdBy;
    }

    public Long created() {
        return created;
    }
}
