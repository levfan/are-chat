package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 思念速递（F53）：点一下「想你了」，小助手在 5~30 分钟后的随机时刻替你说出口——
 * 让「被惦记」变成一个不确定却一定会发生的惊喜。
 */
@Data
@TableName("couple_miss_express")
public class CoupleMissExpress {

    /** 最小延迟 5 分钟 */
    public static final long DELAY_MIN_MS = 5L * 60 * 1000;
    /** 最大延迟 30 分钟 */
    public static final long DELAY_MAX_MS = 30L * 60 * 1000;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private Long deliverAt;
    private boolean delivered;
    private Long deliveredAt;
    private Long created;

    public static CoupleMissExpress of(String spaceId, String fromUser, long deliverAt) {
        CoupleMissExpress row = new CoupleMissExpress();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.deliverAt = deliverAt;
        row.delivered = false;
        row.created = System.currentTimeMillis();
        return row;
    }
}
