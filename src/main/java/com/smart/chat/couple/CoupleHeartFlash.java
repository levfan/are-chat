package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 心动闪光捕捉（F172）：突然心动的一刻速记。 */
@Data
@TableName("couple_heart_flash")
public class CoupleHeartFlash {

    public static final int MOMENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String moment;
    private Long created;

    public static CoupleHeartFlash of(String spaceId, String fromUser, String moment) {
        CoupleHeartFlash row = new CoupleHeartFlash();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.moment = moment;
        row.created = System.currentTimeMillis();
        return row;
    }
}
