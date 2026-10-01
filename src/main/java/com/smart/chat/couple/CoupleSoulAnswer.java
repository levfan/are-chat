package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 灵魂提问盲盒（F165）：每天一个深刻问题，双答才互见。 */
@Data
@TableName("couple_soul_answer")
public class CoupleSoulAnswer {

    public static final int ANSWER_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String answer;
    private Long created;

    public static CoupleSoulAnswer of(String spaceId, String day, String fromUser, String answer) {
        CoupleSoulAnswer row = new CoupleSoulAnswer();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.answer = answer;
        row.created = System.currentTimeMillis();
        return row;
    }
}
