package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 家务积分流水（F186）：做家务赚积分，兑换小奖励。 */
@Data
@TableName("couple_point_ledger")
public class CouplePointLedgerPO {

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_SPEND = "SPEND";

    public static final int ITEM_MAX = 100;
    public static final int POINTS_MIN = 1;
    public static final int POINTS_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** EARN 赚 / SPEND 花 */
    private String type;
    private String item;
    private Integer points;
    private Long created;

    public static CouplePointLedgerPO of(String spaceId, String fromUser, String type, String item, int points) {
        CouplePointLedgerPO row = new CouplePointLedgerPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.type = type;
        row.item = item;
        row.points = points;
        row.created = System.currentTimeMillis();
        return row;
    }
}
