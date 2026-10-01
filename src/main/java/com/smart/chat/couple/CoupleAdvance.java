package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F277 垫付本：互欠结算台账（区别于 F5 日常记账）。 */
@Data
@TableName("couple_advance")
public class CoupleAdvance {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SETTLED = "SETTLED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String item;
    private String payerUser;
    private Integer amountCents;
    private String note;
    private String status;
    private Long settledAt;
    private Long created;

    public static CoupleAdvance of(String spaceId, String item, String payerUser,
                                   int amountCents, String note) {
        CoupleAdvance row = new CoupleAdvance();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.item = item;
        row.payerUser = payerUser;
        row.amountCents = amountCents;
        row.note = note == null ? "" : note;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
