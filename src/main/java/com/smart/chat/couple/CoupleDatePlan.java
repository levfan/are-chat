package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 约会规划卡：把「下次一起」变成有日期的约定，完成后归档成回忆。 */
@Data
@TableName("couple_date_plan")
public class CoupleDatePlan {

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_DONE = "DONE";

    public static final int TITLE_MAX = 60;
    public static final int PLACE_MAX = 100;
    public static final int ITEMS_MAX = 500;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 约会日期（yyyy-MM-dd） */
    private String planDay;
    private String place;
    /** 想做的事（换行分隔） */
    private String items;
    private String status;
    private Long doneAt;
    private String createdBy;
    private Long created;

    public static CoupleDatePlan of(String spaceId, String createdBy, String title, String planDay,
                                    String place, String items) {
        CoupleDatePlan row = new CoupleDatePlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.createdBy = createdBy;
        row.title = title;
        row.planDay = planDay;
        row.place = place;
        row.items = items;
        row.status = STATUS_PLANNED;
        row.created = System.currentTimeMillis();
        return row;
    }
}
