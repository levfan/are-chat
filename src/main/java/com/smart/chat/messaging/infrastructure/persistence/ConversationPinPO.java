package com.smart.chat.messaging.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 84 会话内置顶消息：每个会话（双方）最多一条，双方可见，pin 一方即可。
 * user_a/user_b 为规范化后的双方用户名（字典序小者在前），保证唯一。
 */
@Data
@TableName("conversation_pin")
public class ConversationPinPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String userA;
    private String userB;
    private String msgId;
    private String createdBy;
    private Long created;

    public static ConversationPinPO of(String userA, String userB, String msgId, String createdBy) {
        ConversationPinPO pin = new ConversationPinPO();
        pin.id = UUID.randomUUID().toString();
        pin.userA = userA;
        pin.userB = userB;
        pin.msgId = msgId;
        pin.createdBy = createdBy;
        pin.created = System.currentTimeMillis();
        return pin;
    }
}
