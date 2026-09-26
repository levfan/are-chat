package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 共同日历纪念日：纪念日、生日、约会日。yearly=1 表示每年重复（生日/周年），
 * yearly=0 表示仅当年某天的一次性约会安排。
 */
@Data
@TableName("couple_anniversary")
public class CoupleAnniversary {

    public static final int TITLE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 日期 yyyy-MM-dd */
    private String eventDate;
    private Integer yearly;
    private String createdBy;
    private Long created;

    public static CoupleAnniversary of(String spaceId, String title, String date, boolean yearly, String createdBy) {
        CoupleAnniversary row = new CoupleAnniversary();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.eventDate = date;
        row.yearly = yearly ? 1 : 0;
        row.createdBy = createdBy;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isYearly() {
        return Integer.valueOf(1).equals(yearly);
    }
}
