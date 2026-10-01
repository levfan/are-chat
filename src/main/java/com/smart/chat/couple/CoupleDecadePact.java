package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 十年之约（F122）：双方各写一句「十年后的我们」，都写了就并排展示。 */
@Data
@TableName("couple_decade_pact")
public class CoupleDecadePact {

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private Long created;

    public static CoupleDecadePact of(String spaceId, String fromUser, String content) {
        CoupleDecadePact row = new CoupleDecadePact();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
