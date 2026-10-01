package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 世界情话课（F134）：收藏全世界的情话，今日一课由静态库按日推送。 */
@Data
@TableName("couple_love_word")
public class CoupleLoveWord {

    public static final int WORD_MAX = 100;
    public static final int MEANING_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String word;
    private String meaning;
    private Long created;

    public static CoupleLoveWord of(String spaceId, String fromUser, String word, String meaning) {
        CoupleLoveWord row = new CoupleLoveWord();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.word = word;
        row.meaning = meaning;
        row.created = System.currentTimeMillis();
        return row;
    }
}
