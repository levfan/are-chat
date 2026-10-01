package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 纪念日策划案（F188）：谁策划+点子+落地状态。 */
@Data
@TableName("couple_anniv_plan")
public class CoupleAnnivPlan {

    public static final String STATUS_IDEA = "IDEA";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_DONE = "DONE";

    public static final int TITLE_MAX = 60;
    public static final int IDEA_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 纪念日日期（yyyy-MM-dd） */
    private String day;
    private String title;
    /** 策划人 */
    private String planner;
    private String idea;
    private String status;
    private Long updatedAt;
    private Long created;

    public static CoupleAnnivPlan of(String spaceId, String day, String title, String planner, String idea) {
        CoupleAnnivPlan row = new CoupleAnnivPlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.title = title;
        row.planner = planner;
        row.idea = idea == null ? "" : idea;
        row.status = STATUS_IDEA;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
