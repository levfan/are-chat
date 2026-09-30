package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情话储蓄罐（F69）：平时把情话存进罐子，每天晚上随机取一句「利息」推给 TA。 */
@Data
@TableName("couple_love_bank")
public class CoupleLoveBank {

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private boolean delivered;
    private Long deliveredAt;
    private Long created;

    public static CoupleLoveBank of(String spaceId, String fromUser, String content) {
        CoupleLoveBank row = new CoupleLoveBank();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.delivered = false;
        row.created = System.currentTimeMillis();
        return row;
    }
}
