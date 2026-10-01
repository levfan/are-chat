package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F223 喝水接力：我喝一杯=给 TA 的杯子加一格，3h 未回应亮轻提醒。 */
@Data
@TableName("couple_cozy_water")
public class CoupleCozyWater {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer cups;
    private Long created;
    private Long updatedAt;

    public static CoupleCozyWater of(String spaceId, String day, String fromUser) {
        CoupleCozyWater row = new CoupleCozyWater();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.cups = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
