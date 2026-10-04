package com.smart.chat.messaging.domain.star;

import java.util.UUID;

/**
 * 一条收藏：{@code 收藏人 + 消息} 二元组唯一，<b>个人视角、跨会话</b>。
 * <p>
 * 刻意保持薄（05 第 2.2 条）：收藏没有状态迁移，「收藏归收藏人自己、别人看不见」这件事
 * 由每一条查询都带上 {@code username} 条件保证，不在这里编造归属方法。
 * 收藏指向的消息被删掉时静默跳过（收藏夹是消息的引用，不是外键约束）。
 */
public final class MessageStar {

    private final String id;
    private final String username;
    private final String msgId;
    private final Long created;

    private MessageStar(String id, String username, String msgId, Long created) {
        this.id = id;
        this.username = username;
        this.msgId = msgId;
        this.created = created;
    }

    /** 收一条：收藏时间决定列表顺序（新→旧）。 */
    public static MessageStar add(String username, String msgId, long at) {
        return new MessageStar(UUID.randomUUID().toString(), username, msgId, at);
    }

    /** 从存储重建：不校验。 */
    public static MessageStar restore(String id, String username, String msgId, Long created) {
        return new MessageStar(id, username, msgId, created);
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String msgId() {
        return msgId;
    }

    public Long created() {
        return created;
    }
}
