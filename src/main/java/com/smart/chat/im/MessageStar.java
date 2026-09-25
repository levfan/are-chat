package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 收藏的消息（个人视角，跨会话）。
 */
@TableName("message_star")
public class MessageStar {

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;
    private String username;
    private String msgId;
    private Long created;

    public MessageStar() {
    }

    public static MessageStar of(String username, String msgId) {
        MessageStar row = new MessageStar();
        row.id = UUID.randomUUID().toString();
        row.username = username;
        row.msgId = msgId;
        row.created = System.currentTimeMillis();
        return row;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMsgId() {
        return msgId;
    }

    public Long getCreated() {
        return created;
    }
}
