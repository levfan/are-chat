package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F295 许愿井周问：每周一题双写。 */
@Data
@TableName("couple_well_qa")
public class CoupleWellQa {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String question;
    private String answer;
    private Long created;
    private Long updatedAt;

    public static CoupleWellQa of(String spaceId, String week, String fromUser, String question, String answer) {
        CoupleWellQa row = new CoupleWellQa();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.question = question;
        row.answer = answer;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
