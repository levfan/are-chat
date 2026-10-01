package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F310 体征互报：一天一行数值，超自设线自动推 TA。 */
@Data
@TableName("couple_body_metric")
public class CoupleBodyMetric {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String temp;
    private String weight;
    private String sleepHours;
    private String tempLimit;
    private String sleepLimit;
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyMetric of(String spaceId, String day, String fromUser) {
        CoupleBodyMetric row = new CoupleBodyMetric();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.temp = "";
        row.weight = "";
        row.sleepHours = "";
        row.tempLimit = "";
        row.sleepLimit = "";
        row.note = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 数值解析（坏文本按未填处理）。 */
    static Double num(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
