package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 心愿互换（F73）：我的小心愿交给 TA，TA 的心愿我来实现。 */
@Data
@TableName("couple_wish_exchange")
public class CoupleWishExchange {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DONE = "DONE";
    public static final int WISH_MAX = 200;
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String wish;
    private String status;
    private Long acceptedAt;
    private String doneNote;
    private Long doneAt;
    private Long created;

    public static CoupleWishExchange of(String spaceId, String fromUser, String wish) {
        CoupleWishExchange row = new CoupleWishExchange();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.wish = wish;
        row.status = STATUS_PENDING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
