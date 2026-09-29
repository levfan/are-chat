package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 今日一问互评：对 TA 回答的表情反应，每人每天一条（F48）。 */
@Data
@TableName("couple_answer_reaction")
public class CoupleAnswerReaction {

    public static final int EMOJI_MAX = 16;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 一问日期 yyyy-MM-dd */
    private String answerDay;
    private String fromUser;
    private String emoji;
    private Long created;

    public static CoupleAnswerReaction of(String spaceId, String answerDay, String fromUser, String emoji) {
        CoupleAnswerReaction row = new CoupleAnswerReaction();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.answerDay = answerDay;
        row.fromUser = fromUser;
        row.emoji = emoji;
        row.created = System.currentTimeMillis();
        return row;
    }
}
