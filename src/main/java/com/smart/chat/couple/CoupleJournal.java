package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 贴纸手账（F166）：emoji 贴纸+一句话，每天一页可改。 */
@Data
@TableName("couple_journal")
public class CoupleJournal {

    public static final String DEFAULT_STICKER = "✨";
    public static final int TEXT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String sticker;
    private String text;
    private Long updatedAt;
    private Long created;

    public static CoupleJournal of(String spaceId, String day, String fromUser, String sticker, String text) {
        long now = System.currentTimeMillis();
        CoupleJournal row = new CoupleJournal();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.sticker = sticker;
        row.text = text;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
