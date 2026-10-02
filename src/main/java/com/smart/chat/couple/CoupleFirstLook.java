package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F286 第一眼对视：双盲提交，一致或各满 3 次自动互见。 */
@Data
@TableName("couple_first_look")
public class CoupleFirstLook {

    public static final int TRIES_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String moment;
    private Integer tries;
    private Integer revealed;
    private Long created;
    private Long updatedAt;

    public static CoupleFirstLook of(String spaceId, String fromUser, String moment) {
        CoupleFirstLook row = new CoupleFirstLook();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.moment = moment;
        row.tries = 1;
        row.revealed = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean revealedFlag() {
        return Integer.valueOf(1).equals(revealed);
    }
}
