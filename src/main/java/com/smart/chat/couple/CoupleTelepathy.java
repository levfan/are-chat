package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 心灵感应（F68）：双方各自回答同一道题（不许商量），都答完自动结算——
 * 一致即「感应成功」，不一致也能看到彼此的脑回路。A/B 答案按 space.userA/userB 归档。
 */
@Data
@TableName("couple_telepathy")
public class CoupleTelepathy {

    public static final int ROUND_MAX = 3;
    public static final int ANSWER_MAX = 50;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    /** 当天第几轮（1-3） */
    private int round;
    private String question;
    private String answerA;
    private String answerB;
    private Long created;

    public static CoupleTelepathy of(String spaceId, String question) {
        CoupleTelepathy row = new CoupleTelepathy();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = LocalDate.now().toString();
        row.question = question;
        row.created = System.currentTimeMillis();
        return row;
    }

    /** 双方都已作答。 */
    public boolean bothAnswered() {
        return answerA != null && !answerA.isBlank() && answerB != null && !answerB.isBlank();
    }

    /** 是否感应成功（忽略大小写与首尾空格）。 */
    public boolean matched() {
        return bothAnswered() && answerA.trim().equalsIgnoreCase(answerB.trim());
    }
}
