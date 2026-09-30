package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 幸运签（F56）：每天可以为 TA 抽一支签，把好运寄给对方（每人每天一支，可重抽覆盖）。 */
@Data
@TableName("couple_fortune_slip")
public class CoupleFortuneSlip {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 抽签人（签是抽给对方的） */
    private String fromUser;
    /** 抽签日期 yyyy-MM-dd */
    private String day;
    private String slipKey;
    private String content;
    /** 签的等级（大吉/中吉/小吉/锦鲤） */
    private String level;
    private Long created;

    public static CoupleFortuneSlip of(String spaceId, String fromUser, String day,
                                       String slipKey, String content, String level) {
        CoupleFortuneSlip row = new CoupleFortuneSlip();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.slipKey = slipKey;
        row.content = content;
        row.level = level;
        row.created = System.currentTimeMillis();
        return row;
    }
}
