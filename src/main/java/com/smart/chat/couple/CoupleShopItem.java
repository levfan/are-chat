package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F271 采买清单：超市清单一条，买了打勾推 TA。 */
@Data
@TableName("couple_shop_item")
public class CoupleShopItem {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String qty;
    private String fromUser;
    private String status;
    private String doneBy;
    private Long doneAt;
    private Long created;

    public static CoupleShopItem of(String spaceId, String name, String qty, String fromUser) {
        CoupleShopItem row = new CoupleShopItem();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.qty = qty == null ? "" : qty;
        row.fromUser = fromUser;
        row.status = STATUS_OPEN;
        row.doneBy = "";
        row.created = System.currentTimeMillis();
        return row;
    }
}
