package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F214 本周菜单：一天一个格子排一顿正餐，双方可见可改。 */
@Data
@TableName("couple_dine_weekplan")
public class CoupleDineWeekplan {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String day;
    private String dish;
    private String updatedBy;
    private Long updatedAt;
    private Long created;

    public static CoupleDineWeekplan of(String spaceId, String week, String day, String dish, String updatedBy) {
        CoupleDineWeekplan row = new CoupleDineWeekplan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.day = day;
        row.dish = dish;
        row.updatedBy = updatedBy;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
