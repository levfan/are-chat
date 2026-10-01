package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 一百问（F130）：两人各答 100 道小问题，答完互相可见。 */
@Data
@TableName("couple_survey_answer")
public class CoupleSurveyAnswer {

    public static final int Q_MAX = 100;
    public static final int ANSWER_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private Integer qNo;
    private String answer;
    private Long updatedAt;
    private Long created;

    public static CoupleSurveyAnswer of(String spaceId, String fromUser, int qNo, String answer) {
        long now = System.currentTimeMillis();
        CoupleSurveyAnswer row = new CoupleSurveyAnswer();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.qNo = qNo;
        row.answer = answer;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
