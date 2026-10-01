package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** F312 周期共览：本人标当天阶段与不适，TA 递照顾卡。 */
@Data
@TableName("couple_body_cycle")
public class CoupleBodyCycle {

    public static final List<String> PHASES = List.of("BEFORE", "MENSTRUATING", "AFTER", "OWULARE");

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String phase;
    private String discomfort;
    private String careCard;
    private String careBy;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyCycle of(String spaceId, String day, String fromUser, String phase, String discomfort) {
        CoupleBodyCycle row = new CoupleBodyCycle();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.phase = phase;
        row.discomfort = discomfort == null ? "" : discomfort;
        row.careCard = "";
        row.careBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
