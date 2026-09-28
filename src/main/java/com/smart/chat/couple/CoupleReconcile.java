package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 和好卡：小别扭的温柔收尾——吵架后主动递一张卡（附一句和好留言），
 * 对方点「抱一下，和好」即接受；可记录这次别扭从什么时候开始，接受后算出和好耗时。
 */
@Data
@TableName("couple_reconcile")
public class CoupleReconcile {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_ACCEPTED = "ACCEPTED";

    public static final int MESSAGE_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 递卡人用户名 */
    private String fromUser;
    private String message;
    /** 这次别扭开始时间（可空） */
    private Long startAt;
    private String status;
    private String acceptedBy;
    private Long acceptedAt;
    private Long created;

    public static CoupleReconcile of(String spaceId, String fromUser, String message, Long startAt) {
        CoupleReconcile card = new CoupleReconcile();
        card.id = UUID.randomUUID().toString();
        card.spaceId = spaceId;
        card.fromUser = fromUser;
        card.message = message;
        card.startAt = startAt;
        card.status = STATUS_SENT;
        card.created = System.currentTimeMillis();
        return card;
    }

    /** 和好耗时（小时）：有开始时间且已接受时才可算，向上取整。 */
    public Long durationHours() {
        if (startAt == null || acceptedAt == null || acceptedAt <= startAt) {
            return null;
        }
        return (long) Math.ceil((acceptedAt - startAt) / 3600000.0);
    }
}
