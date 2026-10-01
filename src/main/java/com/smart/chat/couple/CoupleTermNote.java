package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F255 一节气一件事：24 节气手账，本人可改写。 */
@Data
@TableName("couple_term_note")
public class CoupleTermNote {

    public static final int TEXT_MAX = 140;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String term;
    private String year;
    private String fromUser;
    private String text;
    private Long created;
    private Long updatedAt;

    public static CoupleTermNote of(String spaceId, String term, String year, String fromUser, String text) {
        CoupleTermNote row = new CoupleTermNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.term = term;
        row.year = year;
        row.fromUser = fromUser;
        row.text = text;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
