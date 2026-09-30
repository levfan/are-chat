package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情绪词汇足迹（F108）：每天每人一个词形容今天，攒情绪词云。 */
@Data
@TableName("couple_feeling_word")
public class CoupleFeelingWord {

    public static final int WORD_MAX = 20;
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String day;
    private String word;
    private String note;
    private Long created;

    public static CoupleFeelingWord of(String spaceId, String fromUser, String day,
                                       String word, String note) {
        CoupleFeelingWord row = new CoupleFeelingWord();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.word = word;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
