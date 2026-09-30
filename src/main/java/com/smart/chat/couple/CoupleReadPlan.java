package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 共读计划（F74）：同一本书，各自翻页，一起到达结局。 */
@Data
@TableName("couple_read_plan")
public class CoupleReadPlan {

    public static final String STATUS_READING = "READING";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final int TITLE_MAX = 100;
    public static final int UNIT_MAX = 100000;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private int totalUnits;
    private String unitLabel;
    private String status;
    private Long finishedAt;
    private Long created;

    public static CoupleReadPlan of(String spaceId, String title, int totalUnits, String unitLabel) {
        CoupleReadPlan row = new CoupleReadPlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.totalUnits = totalUnits;
        row.unitLabel = unitLabel;
        row.status = STATUS_READING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
