package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F236 愿望券：手动发/核销（OPEN→USED），保险柜 payout 走 ref 标记。 */
@Data
@TableName("couple_ceremony_coupon")
public class CoupleCeremonyCouponPO {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_USED = "USED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private String status;
    private String ref;
    private String issuer;
    private String usedBy;
    private Long usedAt;
    private Long created;

    public static CoupleCeremonyCouponPO of(String spaceId, String title, String issuer, String ref) {
        CoupleCeremonyCouponPO row = new CoupleCeremonyCouponPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.status = STATUS_OPEN;
        row.ref = ref == null ? "" : ref;
        row.issuer = issuer;
        row.usedBy = "";
        row.usedAt = 0L;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }
}
