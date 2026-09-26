package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 收藏的消息（个人视角，跨会话）。
 */
@Data
@TableName("message_star")
public class MessageStar {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String username;
    private String msgId;
    private Long created;

    public static MessageStar of(String username, String msgId) {
        MessageStar row = new MessageStar();
        row.id = UUID.randomUUID().toString();
        row.username = username;
        row.msgId = msgId;
        row.created = System.currentTimeMillis();
        return row;
    }
}
