package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 夸夸墙：把欣赏说出口——写一张具体的夸夸小卡片贴上墙，
 * 对方点「收到啦」后卡片标记为已收到，被夸的瞬间值得被认真签收。
 */
@Data
@TableName("couple_praise")
public class CouplePraise {

    public static final String STATUS_POSTED = "POSTED";
    public static final String STATUS_RECEIVED = "RECEIVED";

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 夸人用户名 */
    private String fromUser;
    private String content;
    private String status;
    private Long receivedAt;
    private Long created;

    public static CouplePraise of(String spaceId, String fromUser, String content) {
        CouplePraise praise = new CouplePraise();
        praise.id = UUID.randomUUID().toString();
        praise.spaceId = spaceId;
        praise.fromUser = fromUser;
        praise.content = content;
        praise.status = STATUS_POSTED;
        praise.created = System.currentTimeMillis();
        return praise;
    }
}
