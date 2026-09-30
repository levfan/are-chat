package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** 双人挑战赛（F70）：每天同一道小挑战，双方各自打卡，双完成即达成。 */
@Data
@TableName("couple_challenge")
public class CoupleChallenge {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String taskText;
    private boolean doneA;
    private boolean doneB;
    private Long doneAtA;
    private Long doneAtB;
    private Long created;

    public static CoupleChallenge of(String spaceId, String day, String taskText) {
        CoupleChallenge row = new CoupleChallenge();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.taskText = taskText;
        row.created = System.currentTimeMillis();
        return row;
    }

    /** 双方都完成 = 挑战达成。 */
    public boolean bothDone() {
        return doneA && doneB;
    }
}
