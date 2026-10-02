package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F355 高光重放：moment/did/feel 三行卡，每人 ≤12 条，本人可删。 */
@Data
@TableName("couple_echo_highlight")
public class CoupleEchoHighlight {

    public static final int MOMENT_MAX = 40;
    public static final int DID_MAX = 80;
    public static final int FEEL_MAX = 80;
    public static final int CAP = 12;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String moment;
    private String did;
    private String feel;
    private Long created;
    private Long updatedAt;

    public static CoupleEchoHighlight of(String spaceId, String fromUser, String moment, String did, String feel) {
        CoupleEchoHighlight row = new CoupleEchoHighlight();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.moment = moment;
        row.did = did;
        row.feel = feel;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
