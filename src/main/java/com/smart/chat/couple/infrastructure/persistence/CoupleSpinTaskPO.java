package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F270 家务轮盘：周锚分派的一条事项，双签生效、完成打勾。 */
@Data
@TableName("couple_spin_task")
public class CoupleSpinTaskPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String item;
    private String assignedUser;
    private Integer confirmed;
    private Integer done;
    private Long doneAt;
    private Long created;

    public static CoupleSpinTaskPO of(String spaceId, String week, String item, String assignedUser) {
        CoupleSpinTaskPO row = new CoupleSpinTaskPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.item = item;
        row.assignedUser = assignedUser;
        row.confirmed = 0;
        row.done = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean confirmedFlag() {
        return Integer.valueOf(1).equals(confirmed);
    }

    public boolean doneFlag() {
        return Integer.valueOf(1).equals(done);
    }
}
