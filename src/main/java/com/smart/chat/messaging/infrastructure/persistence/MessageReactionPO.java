package com.smart.chat.messaging.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 消息表情回应：一条消息 + 一个用户 + 一个表情唯一，toggle 语义。
 */
@Data
@TableName("message_reaction")
public class MessageReactionPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String msgId;
    private String username;
    private String emoji;
    private Long created;

    public static MessageReactionPO of(String msgId, String username, String emoji) {
        MessageReactionPO row = new MessageReactionPO();
        row.id = UUID.randomUUID().toString();
        row.msgId = msgId;
        row.username = username;
        row.emoji = emoji;
        row.created = System.currentTimeMillis();
        return row;
    }
}
