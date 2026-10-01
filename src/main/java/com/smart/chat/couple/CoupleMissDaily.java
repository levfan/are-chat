package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 想念计量所（F112）：每天点亮「今天想你了」，同天互想 = 双向奔赴。 */
@Data
@TableName("couple_miss_daily")
public class CoupleMissDaily {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private Integer missA;
    private Integer missB;
    private Long missAtA;
    private Long missAtB;
    private Long bothAt;
    private Long created;

    public static CoupleMissDaily of(String spaceId, String day) {
        CoupleMissDaily row = new CoupleMissDaily();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.missA = 0;
        row.missB = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean bothMiss() {
        return Integer.valueOf(1).equals(missA) && Integer.valueOf(1).equals(missB);
    }
}
