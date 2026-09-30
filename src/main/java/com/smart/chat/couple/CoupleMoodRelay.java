package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情绪接力棒（F102）：把心情抛给 TA，TA 接住回应并抛回新的心情。 */
@Data
@TableName("couple_mood_relay")
public class CoupleMoodRelay {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CAUGHT = "CAUGHT";
    public static final int MOOD_MAX = 20;
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String moodWord;
    private String moodEmoji;
    private String note;
    private String status;
    private String catchNote;
    private Long caughtAt;
    private Long created;

    public static CoupleMoodRelay of(String spaceId, String fromUser, String moodWord,
                                     String moodEmoji, String note) {
        CoupleMoodRelay row = new CoupleMoodRelay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.moodWord = moodWord;
        row.moodEmoji = moodEmoji;
        row.note = note;
        row.status = STATUS_PENDING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
