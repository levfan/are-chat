package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 梦境手账（F141）：记下梦到 TA 的梦，对方醒来读。 */
@Data
@TableName("couple_dream")
public class CoupleDream {

    public static final int CONTENT_MAX = 500;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private Long created;

    public static CoupleDream of(String spaceId, String fromUser, String content) {
        CoupleDream row = new CoupleDream();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
