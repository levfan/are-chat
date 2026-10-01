package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 心动日历（F177）：每日标记心动等级 1-3。 */
@Data
@TableName("couple_heart_day")
public class CoupleHeartDay {

    public static final int LEVEL_MIN = 1;
    public static final int LEVEL_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer level;
    private Long updatedAt;
    private Long created;

    public static CoupleHeartDay of(String spaceId, String day, String fromUser, int level) {
        long now = System.currentTimeMillis();
        CoupleHeartDay row = new CoupleHeartDay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.level = level;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
