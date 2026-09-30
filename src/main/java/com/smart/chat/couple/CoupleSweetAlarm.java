package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 心动闹钟（F52）：把一句想说的话设成未来的闹钟，到点由小助手替你送达。 */
@Data
@TableName("couple_sweet_alarm")
public class CoupleSweetAlarm {

    public static final int MESSAGE_MAX = 200;
    /** 最多提前 24 小时设定 */
    public static final long HORIZON_MS = 24L * 60 * 60 * 1000;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String message;
    /** 触发时间（毫秒） */
    private Long fireAt;
    private boolean fired;
    private Long firedAt;
    private Long created;

    public static CoupleSweetAlarm of(String spaceId, String fromUser, String message, long fireAt) {
        CoupleSweetAlarm row = new CoupleSweetAlarm();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.message = message;
        row.fireAt = fireAt;
        row.fired = false;
        row.created = System.currentTimeMillis();
        return row;
    }
}
