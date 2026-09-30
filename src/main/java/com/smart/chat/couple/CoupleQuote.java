package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 甜蜜语录收藏册（F83）：甜话会过期，收藏不会。 */
@Data
@TableName("couple_quote")
public class CoupleQuote {

    public static final int CONTENT_MAX = 300;
    public static final int CONTEXT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String context;
    private Long created;

    public static CoupleQuote of(String spaceId, String fromUser, String content, String context) {
        CoupleQuote row = new CoupleQuote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.context = context;
        row.created = System.currentTimeMillis();
        return row;
    }
}
