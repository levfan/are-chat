package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 真心话抽签（F66）：每天一题双方必答，把平时不敢问的问出来。 */
@Data
@TableName("couple_truth")
public class CoupleTruth {

    public static final int ANSWER_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String question;
    private String answerer;
    private String answer;
    private Long created;

    public static CoupleTruth of(String spaceId, String day, String question, String answerer, String answer) {
        CoupleTruth row = new CoupleTruth();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.question = question;
        row.answerer = answerer;
        row.answer = answer;
        row.created = System.currentTimeMillis();
        return row;
    }
}
