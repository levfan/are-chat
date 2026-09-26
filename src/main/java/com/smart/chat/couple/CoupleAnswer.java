package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 今日一问回答：每天自动推一个情侣问题（按日期从题库轮换），双方回答后拼在一起看。
 * 同一天同一人重复提交视为修改回答。
 */
@Data
@TableName("couple_answer")
public class CoupleAnswer {

    /** 回答内容最长长度 */
    public static final int ANSWER_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 问题日期 yyyy-MM-dd */
    private String answerDay;
    private String username;
    private String answer;
    private Long created;

    public static CoupleAnswer of(String spaceId, String day, String username, String answer) {
        CoupleAnswer row = new CoupleAnswer();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.answerDay = day;
        row.username = username;
        row.answer = answer;
        row.created = System.currentTimeMillis();
        return row;
    }
}
