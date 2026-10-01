package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 心情漂流瓶（F163）：坏心情写下来扔出去，对方捡到回信。 */
@Data
@TableName("couple_drift_bottle")
public class CoupleDriftBottle {

    public static final String STATUS_FLOATING = "FLOATING";
    public static final String STATUS_REPLIED = "REPLIED";
    public static final int CONTENT_MAX = 300;
    public static final int REPLY_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String mood;
    private String content;
    private String reply;
    private Long repliedAt;
    private String status;
    private Long created;

    public static CoupleDriftBottle of(String spaceId, String fromUser, String mood, String content) {
        CoupleDriftBottle row = new CoupleDriftBottle();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.mood = mood;
        row.content = content;
        row.status = STATUS_FLOATING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
