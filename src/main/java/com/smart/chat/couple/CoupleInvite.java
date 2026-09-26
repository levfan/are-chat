package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 情侣空间邀请：A 选好友发起 → B 收到推送 → B 同意后建立 couple_space。
 * 一对用户之间同时只允许一条 PENDING 邀请（发起方向任意）。
 */
@Data
@TableName("couple_invite")
public class CoupleInvite {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELED = "CANCELED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String fromUser;
    private String toUser;
    private String message;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleInvite of(String from, String to, String message) {
        CoupleInvite invite = new CoupleInvite();
        invite.id = UUID.randomUUID().toString();
        invite.fromUser = from;
        invite.toUser = to;
        invite.message = message;
        invite.status = STATUS_PENDING;
        invite.created = System.currentTimeMillis();
        return invite;
    }
}
