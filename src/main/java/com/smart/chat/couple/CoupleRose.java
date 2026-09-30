package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 每日玫瑰（F55）：每天限量 3 朵的鲜花与花语——把「我想到你」变成一个小动作。 */
@Data
@TableName("couple_rose")
public class CoupleRose {

    /** 每人每天最多送出的朵数 */
    public static final int DAILY_LIMIT = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 送花日期 yyyy-MM-dd */
    private String day;
    private String flowerKey;
    /** 随花附上的花语 */
    private String word;
    private Long created;

    public static CoupleRose of(String spaceId, String fromUser, String day, String flowerKey, String word) {
        CoupleRose row = new CoupleRose();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.flowerKey = flowerKey;
        row.word = word;
        row.created = System.currentTimeMillis();
        return row;
    }
}
