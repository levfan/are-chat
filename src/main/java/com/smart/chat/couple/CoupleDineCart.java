package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F217 外卖搭伙车：同辆车各加菜，双方都按锁才成行。 */
@Data
@TableName("couple_dine_cart")
public class CoupleDineCart {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String item;
    private Integer qty;
    private String status;
    private String lockedBy;
    private Long created;

    public static CoupleDineCart of(String spaceId, String week, String fromUser, String item, int qty) {
        CoupleDineCart row = new CoupleDineCart();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.item = item;
        row.qty = qty;
        row.status = STATUS_OPEN;
        row.lockedBy = "";
        row.created = System.currentTimeMillis();

        return row;
    }
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_LOCKED = "LOCKED";

}
