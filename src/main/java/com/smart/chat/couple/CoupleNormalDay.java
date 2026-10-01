package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F258 反仪式感日：每年最多 3 天「什么都不做」，当日挡打卡。 */
@Data
@TableName("couple_normal_day")
public class CoupleNormalDay {

    public static final int YEAR_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String day;
    private String fromUser;
    private Long created;

    public static CoupleNormalDay of(String spaceId, String year, String day, String fromUser) {
        CoupleNormalDay row = new CoupleNormalDay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.day = day;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        return row;
    }
}
