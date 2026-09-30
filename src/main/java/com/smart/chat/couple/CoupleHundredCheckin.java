package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 百日之约打卡明细（F72）：每天每人一条，重复提交视为补卡。 */
@Data
@TableName("couple_hundred_checkin")
public class CoupleHundredCheckin {

    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String pactId;
    private String spaceId;
    private String byUser;
    private String day;
    private String note;
    private Long created;

    public static CoupleHundredCheckin of(String pactId, String spaceId, String byUser, String day, String note) {
        CoupleHundredCheckin row = new CoupleHundredCheckin();
        row.id = UUID.randomUUID().toString();
        row.pactId = pactId;
        row.spaceId = spaceId;
        row.byUser = byUser;
        row.day = day;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
