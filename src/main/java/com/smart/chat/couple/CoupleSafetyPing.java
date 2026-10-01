package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 平安卡（F117）：出发/到家一键报平安，对方收到强提醒。 */
@Data
@TableName("couple_safety_ping")
public class CoupleSafetyPing {

    public static final String KIND_GO_OUT = "GO_OUT";
    public static final String KIND_ARRIVE = "ARRIVE";
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String kind;
    private String note;
    private Long created;

    public static CoupleSafetyPing of(String spaceId, String fromUser, String kind, String note) {
        CoupleSafetyPing row = new CoupleSafetyPing();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.kind = kind;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
