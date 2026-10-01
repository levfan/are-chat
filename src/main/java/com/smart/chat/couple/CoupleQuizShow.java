package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F281 默契综艺：一天一期五题填空，双答算默契率。 */
@Data
@TableName("couple_quiz_show")
public class CoupleQuizShow {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String terms;
    private String answerA;
    private String answerB;
    private Long created;
    private Long updatedAt;

    public static CoupleQuizShow of(String spaceId, String day, String terms) {
        CoupleQuizShow row = new CoupleQuizShow();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.terms = terms;
        row.answerA = "";
        row.answerB = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
