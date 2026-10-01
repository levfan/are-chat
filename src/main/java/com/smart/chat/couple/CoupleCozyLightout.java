package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F220 晚安同熄灯：双方都发晚安=当日熄灯，连击满 7 天推里程碑。 */
@Data
@TableName("couple_cozy_lightout")
public class CoupleCozyLightout {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String atTime;
    private Long created;

    public static CoupleCozyLightout of(String spaceId, String day, String fromUser, String atTime) {
        CoupleCozyLightout row = new CoupleCozyLightout();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.atTime = atTime == null ? "" : atTime;
        row.created = System.currentTimeMillis();
        return row;
    }
}
