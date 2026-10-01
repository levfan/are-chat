package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F269 称呼日：今日爱称 Bank 抽取，双方各「用过一次」完成当日。 */
@Data
@TableName("couple_name_day")
public class CoupleNameDay {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String nameText;
    private Integer usedA;
    private Integer usedB;
    private Long created;

    public static CoupleNameDay of(String spaceId, String day, String nameText) {
        CoupleNameDay row = new CoupleNameDay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.nameText = nameText;
        row.usedA = 0;
        row.usedB = 0;
        row.created = System.currentTimeMillis();
        return row;
    }
}
