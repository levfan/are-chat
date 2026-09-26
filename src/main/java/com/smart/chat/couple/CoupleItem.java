package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 共享清单：一起的行程、想看的电影、想去的餐厅、共同待办。
 * 双方都能添加、编辑、打卡完成与删除；带计划日期的事项会出现在共同日历上。
 */
@Data
@TableName("couple_item")
public class CoupleItem {

    public static final String KIND_MOVIE = "MOVIE";
    public static final String KIND_FOOD = "FOOD";
    public static final String KIND_TRIP = "TRIP";
    public static final String KIND_TODO = "TODO";
    public static final int TITLE_MAX = 100;
    public static final int NOTE_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String kind;
    private String title;
    private String note;
    /** 计划日期 yyyy-MM-dd（可空） */
    private String dueDate;
    private Integer done;
    private String doneBy;
    private Long doneAt;
    private String createdBy;
    private Long created;

    public static CoupleItem of(String spaceId, String kind, String title, String createdBy) {
        CoupleItem item = new CoupleItem();
        item.id = UUID.randomUUID().toString();
        item.spaceId = spaceId;
        item.kind = kind;
        item.title = title;
        item.createdBy = createdBy;
        item.done = 0;
        item.created = System.currentTimeMillis();
        return item;
    }

    public boolean isDone() {
        return Integer.valueOf(1).equals(done);
    }
}
