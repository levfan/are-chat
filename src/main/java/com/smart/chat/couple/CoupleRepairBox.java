package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F328 修复礼盒：和好达成掉盒，开出补偿小任务，完成推双方。 */
@Data
@TableName("couple_repair_box")
public class CoupleRepairBox {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String ownerUser;
    private String task;
    private String status;
    private Long doneAt;
    private String fromFreeze;
    private Long created;

    public static CoupleRepairBox of(String spaceId, String day, String ownerUser, String task, String fromFreeze) {
        CoupleRepairBox row = new CoupleRepairBox();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.ownerUser = ownerUser;
        row.task = task;
        row.status = STATUS_OPEN;
        row.fromFreeze = fromFreeze == null ? "" : fromFreeze;
        row.created = System.currentTimeMillis();
        return row;
    }
}
