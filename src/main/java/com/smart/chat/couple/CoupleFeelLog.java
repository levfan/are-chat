package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情绪颗粒度日记（F152）：每日一个细名情绪+强度。 */
@Data
@TableName("couple_feel_log")
public class CoupleFeelLog {

    public static final int WORD_MAX = 20;
    public static final int NOTE_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String day;
    private String word;
    private Integer intensity;
    private String note;
    private Long updatedAt;
    private Long created;

    public static CoupleFeelLog of(String spaceId, String fromUser, String day, String word, int intensity, String note) {
        long now = System.currentTimeMillis();
        CoupleFeelLog row = new CoupleFeelLog();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.word = word;
        row.intensity = intensity;
        row.note = note;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
