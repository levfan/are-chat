package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 愿望清单条目：owner_user 是「想要它的人」，creator_user 是「把它记下来的人」。
 * status 的真值可能是 PREPARED，但对外回显必须经 {@code Wish.visibleStatusFor(me)} 过滤。
 */
@Data
@TableName("couple_wish")
public class CoupleWishPO {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PREPARED = "PREPARED";
    public static final String STATUS_FULFILLED = "FULFILLED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String ownerUser;
    private String creatorUser;
    private String title;
    private String note;
    private String status;
    private String preparedBy;
    private Long preparedAt;
    private Long fulfilledAt;
    private Long created;
    private Long updatedAt;

    public static CoupleWishPO of(String spaceId, String ownerUser, String creatorUser, String title, String note) {
        CoupleWishPO row = new CoupleWishPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.ownerUser = ownerUser;
        row.creatorUser = creatorUser;
        row.title = title;
        row.note = note;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
