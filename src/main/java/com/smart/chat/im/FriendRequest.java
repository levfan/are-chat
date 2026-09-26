package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 好友申请：PENDING / ACCEPTED / REJECTED。
 */
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

    public FriendRequest() {
    }

    public static FriendRequest of(String from, String to) {
        FriendRequest row = new FriendRequest();
        row.id = UUID.randomUUID().toString();
        row.fromUser = from;
        row.toUser = to;
        row.status = STATUS_PENDING;
        row.created = System.currentTimeMillis();
        return row;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
