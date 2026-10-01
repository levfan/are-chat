package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F306 如果我是你爸妈：每日一题，双方都答完才互见。 */
@Data
@TableName("couple_if_family")
public class CoupleIfFamily {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String question;
    private String answerA;
    private String answerB;
    private Long created;
    private Long updatedAt;

    public static CoupleIfFamily of(String spaceId, String day, String question) {
        CoupleIfFamily row = new CoupleIfFamily();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.question = question;
        row.answerA = "";
        row.answerB = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
