package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 第一次清单：恋爱里每个第一次的日子与心情（F46）。 */
@Data
@TableName("couple_first")
public class CoupleFirst {

    public static final int TITLE_MAX = 100;
    public static final int NOTE_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 发生日期 yyyy-MM-dd */
    private String firstDay;
    private String note;
    private String createdBy;
    private Long created;

    public static CoupleFirst of(String spaceId, String title, String firstDay, String note, String createdBy) {
        CoupleFirst row = new CoupleFirst();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.firstDay = firstDay;
        row.note = note;
        row.createdBy = createdBy;
        row.created = System.currentTimeMillis();
        return row;
    }
}
