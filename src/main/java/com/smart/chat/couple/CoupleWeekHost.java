package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 本周主理人（F181）：按周轮流当家，主理人排本周小计划。 */
@Data
@TableName("couple_week_host")
public class CoupleWeekHost {

    public static final int PLAN_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 所属周（周一日期 yyyy-MM-dd） */
    private String week;
    /** 主理人排的本周小计划 */
    private String plan;
    private Long created;
    private Long updatedAt;

    public static CoupleWeekHost of(String spaceId, String week) {
        CoupleWeekHost row = new CoupleWeekHost();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.plan = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
