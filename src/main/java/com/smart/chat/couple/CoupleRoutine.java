package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 我们的作息表（F114）：双方各一份作息时间线，重叠时段高亮。 */
@Data
@TableName("couple_routine")
public class CoupleRoutine {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String ownerUser;
    private String wakeTime;
    private String workStart;
    private String workEnd;
    private String sleepTime;
    private Long updatedAt;
    private Long created;

    public static CoupleRoutine of(String spaceId, String ownerUser,
                                   String wakeTime, String workStart, String workEnd, String sleepTime) {
        CoupleRoutine row = new CoupleRoutine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.ownerUser = ownerUser;
        row.wakeTime = wakeTime;
        row.workStart = workStart;
        row.workEnd = workEnd;
        row.sleepTime = sleepTime;
        row.updatedAt = System.currentTimeMillis();
        row.created = System.currentTimeMillis();
        return row;
    }
}
