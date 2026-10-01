package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 同频共振（F175）：双方 10 秒内先后按键，差值毫秒定默契。 */
@Data
@TableName("couple_sync_tap")
public class CoupleSyncTap {

    public static final long WINDOW_MS = 10_000L;
    public static final long HIT_MS = 500L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private Integer attempts;
    private Long bestMs;
    private String lastTapUser;
    private Long lastTapAt;
    private Integer hits;
    private Long updatedAt;
    private Long created;

    public static CoupleSyncTap of(String spaceId, String day) {
        long now = System.currentTimeMillis();
        CoupleSyncTap row = new CoupleSyncTap();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.attempts = 0;
        row.hits = 0;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
