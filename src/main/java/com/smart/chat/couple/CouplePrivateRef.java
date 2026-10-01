package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F304 黑话大全：收录梗词条 + 抽查对方「谁先忘」。 */
@Data
@TableName("couple_private_ref")
public class CouplePrivateRef {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String term;
    private String meaning;
    private String origin;
    private String fromUser;
    private String quizAnswer;
    private String quizBy;
    private String judged;
    private Long created;
    private Long updatedAt;

    public static CouplePrivateRef of(String spaceId, String term, String meaning, String origin, String fromUser) {
        CouplePrivateRef row = new CouplePrivateRef();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.term = term;
        row.meaning = meaning == null ? "" : meaning;
        row.origin = origin == null ? "" : origin;
        row.fromUser = fromUser;
        row.quizAnswer = "";
        row.quizBy = "";
        row.judged = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
