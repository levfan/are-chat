package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F267 语气翻译官：消息太冷时自报语气，给对方看翻译条（一人一天一条）。 */
@Data
@TableName("couple_tone_note")
public class CoupleToneNote {

    public static final String TONE_TIRED = "TIRED";
    public static final String TONE_BUSY = "BUSY";
    public static final String TONE_SAD = "SAD";
    public static final String TONE_OKAY = "OKAY";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String tone;
    private String note;
    private Long created;

    public static CoupleToneNote of(String spaceId, String day, String fromUser, String tone, String note) {
        CoupleToneNote row = new CoupleToneNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.tone = tone;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
