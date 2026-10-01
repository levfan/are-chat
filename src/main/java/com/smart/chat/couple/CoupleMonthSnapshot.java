package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情侣存档点（F185）：每月快照工作/健康/感情温度，双方对照。 */
@Data
@TableName("couple_month_snapshot")
public class CoupleMonthSnapshot {

    public static final int FIELD_MAX = 100;
    public static final int TEMP_MIN = 1;
    public static final int TEMP_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 所属月份（yyyy-MM） */
    private String month;
    private String fromUser;
    private String work;
    private String health;
    /** 感情温度 0-100 */
    private Integer loveTemp;
    private Long updatedAt;
    private Long created;

    public static CoupleMonthSnapshot of(String spaceId, String month, String fromUser, String work, String health, int loveTemp) {
        CoupleMonthSnapshot row = new CoupleMonthSnapshot();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.month = month;
        row.fromUser = fromUser;
        row.work = work == null ? "" : work;
        row.health = health == null ? "" : health;
        row.loveTemp = loveTemp;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
