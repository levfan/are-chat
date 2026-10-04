package com.smart.chat.messaging.domain.reaction;

import com.smart.chat.messaging.domain.RuleViolation;

import java.util.Set;
import java.util.UUID;

/**
 * 一条表情回应：{@code 消息 + 用户 + 表情} 三元组唯一，重复提交就是取消（toggle 语义）。
 * <p>
 * 这是 append-only 的流水，没有状态迁移，所以领域类型<b>刻意保持薄</b>（05 第 2.2 条「宁薄勿假」）：
 * 只保留校验过的工厂、访问器和那条唯一性白名单。「一条消息不能重复回同一个表情」这件事由
 * 仓储的 {@code findByMsgIdAndUserAndEmoji} + 唯一索引承担，不在这里编造一个 toggle 方法。
 */
public final class MessageReaction {

    /** 回应支持的表情白名单（情侣风全集 36 个，与前端 REACTION_ALL 严格对齐） */
    public static final Set<String> SUPPORTED = Set.of(
            "❤️", "🥰", "😍", "😘", "🤗", "💕",
            "😂", "🤣", "😆", "😉", "😊", "😌",
            "😮", "😳", "😱", "🥺", "😢", "😭",
            "😤", "😡", "🙁", "😴", "🤔", "😏",
            "😎", "🥳", "👍", "👎", "🙌", "👏",
            "✌️", "🙈", "🔥", "🎉", "🌹", "⛵");

    private final String id;
    private final String msgId;
    private final String username;
    private final String emoji;
    private final Long created;

    private MessageReaction(String id, String msgId, String username, String emoji, Long created) {
        this.id = id;
        this.msgId = msgId;
        this.username = username;
        this.emoji = emoji;
        this.created = created;
    }

    /** 记一条回应：表情必须在白名单内。 */
    public static MessageReaction add(String msgId, String username, String emoji, long at) {
        requireSupported(emoji);
        return new MessageReaction(UUID.randomUUID().toString(), msgId, username, emoji, at);
    }

    /** 从存储重建：不校验，存量数据里可能有白名单收窄前留下的表情。 */
    public static MessageReaction restore(String id, String msgId, String username, String emoji, Long created) {
        return new MessageReaction(id, msgId, username, emoji, created);
    }

    /** 白名单裁决（无状态规则，见 05 第 2.2 条）。 */
    public static void requireSupported(String emoji) {
        if (emoji == null || !SUPPORTED.contains(emoji)) {
            throw com.smart.chat.messaging.domain.RuleViolation.of("不支持的表情回应");
        }
    }

    public String id() {
        return id;
    }

    public String msgId() {
        return msgId;
    }

    public String username() {
        return username;
    }

    public String emoji() {
        return emoji;
    }

    public Long created() {
        return created;
    }
}
