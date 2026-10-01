package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F254 节日家档：8 大节日每年怎么过，一人一年一案，双案对照。 */
@Data
@TableName("couple_festival_plan")
public class CoupleFestivalPlan {

    public static final int PLAN_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String festival;
    private String year;
    private String fromUser;
    private String plan;
    private Long created;
    private Long updatedAt;

    public static CoupleFestivalPlan of(String spaceId, String festival, String year, String fromUser, String plan) {
        CoupleFestivalPlan row = new CoupleFestivalPlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.festival = festival;
        row.year = year;
        row.fromUser = fromUser;
        row.plan = plan;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
