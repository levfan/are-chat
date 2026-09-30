package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 你比划我猜（F103）：一方描述（不能带原词），另一方猜，每天最多 5 轮。 */
@Data
@TableName("couple_guess_round")
public class CoupleGuessRound {

    public static final String STATUS_DRAWN = "DRAWN";
    public static final String STATUS_CLUED = "CLUED";
    public static final String STATUS_HIT = "HIT";
    public static final String STATUS_MISSED = "MISSED";
    /** 每天最多轮数。 */
    public static final int DAILY_ROUNDS = 5;
    /** 每轮最多猜错次数。 */
    public static final int MAX_ATTEMPTS = 3;
    public static final int WORD_MAX = 30;
    public static final int CLUE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String word;
    private String clue;
    private String guess;
    private Integer attempts;
    private String status;
    private Long settledAt;
    private Long created;

    public static CoupleGuessRound of(String spaceId, String day, String fromUser, String word) {
        CoupleGuessRound row = new CoupleGuessRound();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.word = word;
        row.attempts = 0;
        row.status = STATUS_DRAWN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
