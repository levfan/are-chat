package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F222 数羊房：60s 内双方各点满 10 下一起数完一群羊，看默契用时。 */
@Data
@TableName("couple_cozy_sheep")
public class CoupleCozySheep {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer taps;
    private Integer done;
    private Integer elapsedMs;
    private Long created;
    private Long updatedAt;

    public static CoupleCozySheep of(String spaceId, String day, String fromUser) {
        CoupleCozySheep row = new CoupleCozySheep();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.taps = 0;
        row.done = 0;
        row.elapsedMs = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
