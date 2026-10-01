package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F263 本周答不上来的问题：一周一题，对方作答。 */
@Data
@TableName("couple_stuck_q")
public class CoupleStuckQ {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String question;
    private String answer;
    private Long answeredAt;
    private Long created;

    public static CoupleStuckQ of(String spaceId, String week, String fromUser, String question) {
        CoupleStuckQ row = new CoupleStuckQ();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.question = question;
        row.answer = "";
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isAnswered() {
        return answer != null && !answer.isEmpty();
    }
}
