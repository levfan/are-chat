package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 百日之约（F72）：把「想坚持的事」变成 100 天的双人打卡。 */
@Data
@TableName("couple_hundred")
public class CoupleHundred {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_BROKEN = "BROKEN";
    public static final int GOAL_DAYS = 100;
    public static final int GOAL_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String goal;
    private String startDay;
    private String status;
    private Long doneAt;
    private Long created;

    public static CoupleHundred of(String spaceId, String goal, String startDay) {
        CoupleHundred row = new CoupleHundred();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.goal = goal;
        row.startDay = startDay;
        row.status = STATUS_ACTIVE;
        row.created = System.currentTimeMillis();
        return row;
    }
}
