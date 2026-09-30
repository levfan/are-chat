package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 共读进度明细（F74）：各自多次上报最新进度，取每人最新一条。 */
@Data
@TableName("couple_read_progress")
public class CoupleReadProgress {

    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String planId;
    private String fromUser;
    private int unit;
    private String note;
    private Long created;

    public static CoupleReadProgress of(String planId, String fromUser, int unit, String note) {
        CoupleReadProgress row = new CoupleReadProgress();
        row.id = UUID.randomUUID().toString();
        row.planId = planId;
        row.fromUser = fromUser;
        row.unit = unit;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
