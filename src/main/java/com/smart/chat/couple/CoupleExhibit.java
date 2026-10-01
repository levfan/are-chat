package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 恋爱博物馆展品（F191）：旧票根/小物件的文字展品档案。 */
@Data
@TableName("couple_exhibit")
public class CoupleExhibit {

    public static final int NAME_MAX = 60;
    public static final int STORY_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String story;
    /** 藏品日期（可空 yyyy-MM-dd） */
    private String obtainedDay;
    private String fromUser;
    private Long created;

    public static CoupleExhibit of(String spaceId, String fromUser, String name, String story, String obtainedDay) {
        CoupleExhibit row = new CoupleExhibit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.name = name;
        row.story = story == null ? "" : story;
        row.obtainedDay = obtainedDay;
        row.created = System.currentTimeMillis();
        return row;
    }
}
