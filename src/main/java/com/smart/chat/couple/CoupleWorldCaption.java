package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F334 文案代写：各交三候选互评选稿，定稿进百科。 */
@Data
@TableName("couple_world_caption")
public class CoupleWorldCaption {

    public static final int SLOT_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer slot;
    private String text;
    private Integer won;
    private Long created;

    public static CoupleWorldCaption of(String spaceId, String day, String fromUser, int slot, String text) {
        CoupleWorldCaption row = new CoupleWorldCaption();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.slot = slot;
        row.text = text;
        row.won = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean wonFlag() {
        return won != null && won == 1;
    }
}
