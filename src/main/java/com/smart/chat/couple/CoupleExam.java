package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F284 友情测验：给对方出「你记得吗」，答错 7 天后可补考。 */
@Data
@TableName("couple_exam")
public class CoupleExam {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String question;
    private String answer;
    private String quizzedUser;
    private String fromUser;
    private String verdict;
    private String lastTryDay;
    private Long created;

    public static CoupleExam of(String spaceId, String question, String answer,
                                String quizzedUser, String fromUser) {
        CoupleExam row = new CoupleExam();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.question = question;
        row.answer = answer;
        row.quizzedUser = quizzedUser;
        row.fromUser = fromUser;
        row.verdict = "";
        row.lastTryDay = "";
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean doneFlag() {
        return "RIGHT".equals(verdict);
    }
}
