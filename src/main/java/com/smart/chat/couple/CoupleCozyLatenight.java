package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F225 熬夜守护：深夜递一张早点睡陪伴卡，一天一张幂等。 */
@Data
@TableName("couple_cozy_latenight")
public class CoupleCozyLatenight {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Long created;

    public static CoupleCozyLatenight of(String spaceId, String day, String fromUser) {
        CoupleCozyLatenight row = new CoupleCozyLatenight();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        return row;
    }
}
