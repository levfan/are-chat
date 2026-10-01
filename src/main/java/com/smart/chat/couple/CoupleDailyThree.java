package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 每日三问（F145）：今天最开心/最被感动/最想对 TA 说。 */
@Data
@TableName("couple_daily_three")
public class CoupleDailyThree {

    public static final int FIELD_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String day;
    private String joy;
    private String touched;
    private String wantToSay;
    private Long updatedAt;
    private Long created;

    public static CoupleDailyThree of(String spaceId, String fromUser, String day) {
        long now = System.currentTimeMillis();
        CoupleDailyThree row = new CoupleDailyThree();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
