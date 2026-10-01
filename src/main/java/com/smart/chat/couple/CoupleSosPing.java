package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情绪 SOS（F144）：一键发出「现在就要抱抱」，对方抱住即接住。 */
@Data
@TableName("couple_sos_ping")
public class CoupleSosPing {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_HELD = "HELD";
    public static final int MESSAGE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String message;
    private String status;
    private Long heldAt;
    private Long created;

    public static CoupleSosPing of(String spaceId, String fromUser, String message) {
        CoupleSosPing row = new CoupleSosPing();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.message = message;
        row.status = STATUS_SENT;
        row.created = System.currentTimeMillis();
        return row;
    }
}
