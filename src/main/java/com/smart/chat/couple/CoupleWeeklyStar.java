package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 每周高光互评（F153）：每周提名对方的一个高光瞬间。 */
@Data
@TableName("couple_weekly_star")
public class CoupleWeeklyStar {

    public static final int HIGHLIGHT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String highlight;
    private Long created;

    public static CoupleWeeklyStar of(String spaceId, String week, String fromUser, String highlight) {
        CoupleWeeklyStar row = new CoupleWeeklyStar();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.highlight = highlight;
        row.created = System.currentTimeMillis();
        return row;
    }
}
