package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 信任存折（F125）：给对方存信任币，攒的是「我信你」。 */
@Data
@TableName("couple_trust_coin")
public class CoupleTrustCoin {

    public static final int REASON_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String toUser;
    private String reason;
    private Long created;

    public static CoupleTrustCoin of(String spaceId, String fromUser, String toUser, String reason) {
        CoupleTrustCoin row = new CoupleTrustCoin();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.toUser = toUser;
        row.reason = reason;
        row.created = System.currentTimeMillis();
        return row;
    }
}
