package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F272 冰箱库存：同空间同名唯一，用完互相看得到。 */
@Data
@TableName("couple_stock")
public class CoupleStock {

    public static final String STATUS_IN = "IN";
    public static final String STATUS_OUT = "OUT";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String item;
    private String qty;
    private String putDay;
    private String expireDay;
    private String status;
    private String fromUser;
    private Long created;
    private Long updatedAt;

    public static CoupleStock of(String spaceId, String item, String qty, String putDay,
                                 String expireDay, String fromUser) {
        CoupleStock row = new CoupleStock();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.item = item;
        row.qty = qty == null ? "" : qty;
        row.putDay = putDay == null ? "" : putDay;
        row.expireDay = expireDay == null ? "" : expireDay;
        row.status = STATUS_IN;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
