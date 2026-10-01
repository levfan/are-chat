package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F317 体检陪同：约体检 + TA 虚拟陪同到场打卡 + 检后一句话互见。 */
@Data
@TableName("couple_body_checkup")
public class CoupleBodyCheckup {

    public static final String STATUS_PLAN = "PLAN";
    public static final String STATUS_ATTENDED = "ATTENDED";
    public static final String STATUS_REPORTED = "REPORTED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String ownerUser;
    private String item;
    private String status;
    private Integer companion;
    private String report;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyCheckup of(String spaceId, String day, String ownerUser, String item) {
        CoupleBodyCheckup row = new CoupleBodyCheckup();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.ownerUser = ownerUser;
        row.item = item == null ? "" : item;
        row.status = STATUS_PLAN;
        row.companion = 0;
        row.report = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** TA 是否已虚拟陪同到场。 */
    public boolean isCompanioned() {
        return companion != null && companion == 1;
    }
}
