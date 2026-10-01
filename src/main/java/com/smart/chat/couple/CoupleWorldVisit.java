package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F330 拜访攻略：回谁家前置任务卡双确认，回访后写战报。 */
@Data
@TableName("couple_world_visit")
public class CoupleWorldVisit {

    public static final String SIDE_MINE = "MINE";
    public static final String SIDE_YOURS = "YOURS";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String hostSide;
    private String fromUser;
    private String preps;
    private Integer confirmed;
    private String report;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldVisit of(String spaceId, String day, String hostSide, String fromUser, String preps) {
        CoupleWorldVisit row = new CoupleWorldVisit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.hostSide = hostSide;
        row.fromUser = fromUser;
        row.preps = preps == null ? "" : preps;
        row.confirmed = 0;
        row.report = "";
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isConfirmed() {
        return confirmed != null && confirmed == 1;
    }
}
