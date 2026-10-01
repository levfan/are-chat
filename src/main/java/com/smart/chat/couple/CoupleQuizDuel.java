package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 出题考TA（F131）：出题人出问答题，对方作答，出题人人工判分。 */
@Data
@TableName("couple_quiz_duel")
public class CoupleQuizDuel {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ANSWERED = "ANSWERED";
    public static final String STATUS_JUDGED = "JUDGED";
    public static final String VERDICT_RIGHT = "RIGHT";
    public static final String VERDICT_WRONG = "WRONG";
    public static final int QUESTION_MAX = 200;
    public static final int ANSWER_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String question;
    private String answerText;
    private String status;
    private String verdict;
    private Long created;

    public static CoupleQuizDuel of(String spaceId, String fromUser, String question) {
        CoupleQuizDuel row = new CoupleQuizDuel();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.question = question;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
