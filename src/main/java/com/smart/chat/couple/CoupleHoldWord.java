package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F265 早想说：封存队列每 7 天自动放行一句送达（读时惰性结算）。 */
@Data
@TableName("couple_hold_word")
public class CoupleHoldWord {

    public static final String STATUS_HELD = "HELD";
    public static final String STATUS_SENT = "SENT";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String content;
    private String fromUser;
    private String openDay;
    private String status;
    private Long sentAt;
    private Long created;

    public static CoupleHoldWord of(String spaceId, String content, String fromUser, String openDay) {
        CoupleHoldWord row = new CoupleHoldWord();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.content = content;
        row.fromUser = fromUser;
        row.openDay = openDay;
        row.status = STATUS_HELD;
        row.created = System.currentTimeMillis();
        return row;
    }
}
