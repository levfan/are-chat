package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 好友申请：PENDING / ACCEPTED / REJECTED。
 */
@Data
@TableName("friend_request")
public class FriendRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String fromUser;
    private String toUser;
    private String message;
    private String status;
    private Long created;
    private Long updatedAt;

    public static FriendRequest of(String from, String to) {
        FriendRequest row = new FriendRequest();
        row.id = UUID.randomUUID().toString();
        row.fromUser = from;
        row.toUser = to;
        row.status = STATUS_PENDING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
