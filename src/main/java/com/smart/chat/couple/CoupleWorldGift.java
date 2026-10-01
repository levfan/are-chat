package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F331 送礼互助池：收 TA 亲友可能喜欢 + 接单代买 + 节前排雷。 */
@Data
@TableName("couple_world_gift")
public class CoupleWorldGift {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_TAKEN = "TAKEN";
    public static final String STATUS_BOUGHT = "BOUGHT";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String person;
    private String idea;
    private String budget;
    private String avoid;
    private String ownerUser;
    private String takerUser;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldGift of(String spaceId, String person, String idea, String budget,
                                     String avoid, String ownerUser) {
        CoupleWorldGift row = new CoupleWorldGift();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.person = person;
        row.idea = idea;
        row.budget = budget == null ? "" : budget;
        row.avoid = avoid == null ? "" : avoid;
        row.ownerUser = ownerUser;
        row.takerUser = "";
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
