package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 每日一问的回答：每人每天一行，同空间同天同一题（题号与题干都落库留快照）。
 * 双方都答过之后才互相可见，这条可见性规则在 {@code couple.domain.question.DailyQuestion}。
 */
@Data
@TableName("couple_question_answer")
public class CoupleQuestionAnswerPO {

    public static final int ANSWER_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** yyyy-MM-dd */
    private String day;
    /** 题库下标 */
    private Integer questionIndex;
    /** 当日题目快照 */
    private String question;
    private String username;
    private String answer;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestionAnswerPO of(String spaceId, String day, int questionIndex, String question,
                                           String username, String answer) {
        CoupleQuestionAnswerPO row = new CoupleQuestionAnswerPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.questionIndex = questionIndex;
        row.question = question;
        row.username = username;
        row.answer = answer;
        row.created = System.currentTimeMillis();
        return row;
    }
}
