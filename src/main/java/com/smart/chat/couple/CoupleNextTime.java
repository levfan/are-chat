package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 下次一定清单（F79）：随口的承诺不散场，落单可催办。 */
@Data
@TableName("couple_next_time")
public class CoupleNextTime {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";
    public static final int CONTENT_MAX = 200;
    /** 同一条「下次一定」被催的冷却时间（1 小时）。 */
    public static final long NUDGE_COOLDOWN_MS = 60 * 60 * 1000L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String status;
    private Long doneAt;
    private Long nudgedAt;
    private Long created;

    public static CoupleNextTime of(String spaceId, String fromUser, String content) {
        CoupleNextTime row = new CoupleNextTime();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.status = STATUS_PENDING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
