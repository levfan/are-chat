package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 默契大考验对局：同一道趣味题双方背对背作答，第二个人提交后立即结算——
 * 答案一致记一次「心有灵犀」，不一致也可以看看彼此怎么想。
 */
@Data
@TableName("couple_tacit")
public class CoupleTacit {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String question;
    /** 用户 A 的答案（null = 还没答） */
    private String answerA;
    /** 用户 B 的答案（null = 还没答） */
    private String answerB;
    /** 是否默契一致：1 一致 / 0 不一致 / null 待结算 */
    private Integer match;
    private Long created;
    private Long settledAt;

    public static CoupleTacit of(String spaceId, String question) {
        CoupleTacit tacit = new CoupleTacit();
        tacit.id = UUID.randomUUID().toString();
        tacit.spaceId = spaceId;
        tacit.question = question;
        tacit.created = System.currentTimeMillis();
        return tacit;
    }

    public boolean isSettled() {
        return match != null;
    }
}
