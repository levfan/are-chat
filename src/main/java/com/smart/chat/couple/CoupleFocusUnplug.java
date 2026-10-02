package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F365 不插电半小时：睡前 30 分钟无手机共同打卡，周连击读时算（不建定时任务）。
 * 双人列口径同 F360：tick_a 属 CoupleSpace.userA，tick_b 属 userB。
 */
@Data
@TableName("couple_focus_unplug")
public class CoupleFocusUnplug {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    /** 1=userA 点过 */
    private Integer tickA;
    /** 1=userB 点过 */
    private Integer tickB;
    private Long created;
    private Long updatedAt;

    public static CoupleFocusUnplug of(String spaceId, String day) {
        CoupleFocusUnplug row = new CoupleFocusUnplug();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.tickA = 0;
        row.tickB = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean ticked(boolean isA) {
        Integer v = isA ? tickA : tickB;
        return v != null && v == 1;
    }

    /** 双方都点=当天真的不插电了（连击只认双点日）。 */
    public boolean bothTicked() {
        return ticked(true) && ticked(false);
    }

    /** 点自己那一格；返回本次是否真的从 0 变 1（用于只推一次）。 */
    public boolean tick(boolean isA) {
        if (ticked(isA)) {
            return false;
        }
        if (isA) {
            tickA = 1;
        } else {
            tickB = 1;
        }
        updatedAt = System.currentTimeMillis();
        return true;
    }
}
