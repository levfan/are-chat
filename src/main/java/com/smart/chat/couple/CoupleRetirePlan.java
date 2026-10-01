package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F294 退休计划双写：30/40/50 档各写「那时候我们在干嘛」。 */
@Data
@TableName("couple_retire_plan")
public class CoupleRetirePlan {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String ageBand;
    private String fromUser;
    private String text;
    private Long created;
    private Long updatedAt;

    public static CoupleRetirePlan of(String spaceId, String ageBand, String fromUser, String text) {
        CoupleRetirePlan row = new CoupleRetirePlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.ageBand = ageBand;
        row.fromUser = fromUser;
        row.text = text;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
