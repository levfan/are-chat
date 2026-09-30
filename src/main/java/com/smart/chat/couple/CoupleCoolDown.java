package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 冷静角（F101）：吵架停战协议，冷静 30 分钟后双方各留一句软话和好。 */
@Data
@TableName("couple_cool_down")
public class CoupleCoolDown {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HEALED = "HEALED";
    /** 冷静期时长：30 分钟。 */
    public static final long DURATION_MS = 30 * 60 * 1000L;
    public static final int REASON_MAX = 100;
    public static final int SOFT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String reason;
    private String status;
    private Long endAt;
    private String softA;
    private String softB;
    private Long softAtA;
    private Long softAtB;
    private Long healedAt;
    private Long created;

    public static CoupleCoolDown of(String spaceId, String fromUser, String reason) {
        CoupleCoolDown row = new CoupleCoolDown();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.reason = reason;
        row.status = STATUS_ACTIVE;
        row.endAt = System.currentTimeMillis() + DURATION_MS;
        row.created = System.currentTimeMillis();
        return row;
    }

    /** 我这一侧的软话是否已留。 */
    public boolean softened(String me, boolean meIsA) {
        return meIsA ? softA != null : softB != null;
    }
}
