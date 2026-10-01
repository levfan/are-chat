package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F296 时光胶囊接龙：给 TA 写 1/2/3 年后的一笔，到点对方拆。 */
@Data
@TableName("couple_relay_capsule")
public class CoupleRelayCapsule {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String openDay;
    private String status;
    private Long openedAt;
    private Long created;

    public static CoupleRelayCapsule of(String spaceId, String fromUser, String content, String openDay) {
        CoupleRelayCapsule row = new CoupleRelayCapsule();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.openDay = openDay;
        row.status = "SEALED";
        row.created = System.currentTimeMillis();
        return row;
    }
}
