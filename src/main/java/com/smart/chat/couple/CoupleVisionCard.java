package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 愿景板（F123）：一人一张愿景卡，写了同一个词就是共鸣。 */
@Data
@TableName("couple_vision_card")
public class CoupleVisionCard {

    public static final int WORD_MAX = 20;
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String word;
    private String note;
    private Long created;

    public static CoupleVisionCard of(String spaceId, String fromUser, String word, String note) {
        CoupleVisionCard row = new CoupleVisionCard();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.word = word;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
