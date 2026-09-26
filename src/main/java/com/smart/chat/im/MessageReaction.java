package com.smart.chat.im;

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
public class MessageReaction {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String msgId;
    private String username;
    private String emoji;
    private Long created;

    public static MessageReaction of(String msgId, String username, String emoji) {
        MessageReaction row = new MessageReaction();
        row.id = UUID.randomUUID().toString();
        row.msgId = msgId;
        row.username = username;
        row.emoji = emoji;
        row.created = System.currentTimeMillis();
        return row;
    }
}
