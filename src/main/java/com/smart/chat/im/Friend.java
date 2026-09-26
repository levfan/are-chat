package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 好友关系（双向两行：owner=拥有者，friend=好友）。
 * lastReadAt 用于未读红点计算；pinned 置顶；muted 免打扰；lastSeenAt 最近在线时间。
 */
@TableName("friend")
public class Friend {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String ownerUsername;
    private String friendUsername;
    private String remark;
    private String tag;
    private Boolean pinned;
    private Boolean muted;
    private Integer blocked;
    private Long lastReadAt;
    private Long lastSeenAt;
    private Long created;

    public Friend() {
    }

    public static Friend of(String owner, String friend) {
        Friend row = new Friend();
        row.id = UUID.randomUUID().toString();
        row.ownerUsername = owner;
        row.friendUsername = friend;
        row.remark = "";
        row.pinned = false;
        row.muted = false;
        row.lastReadAt = 0L;
        row.created = System.currentTimeMillis();
        return row;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getMuted() {
        return muted;
    }

    public void setMuted(Boolean muted) {
        this.muted = muted;
    }

    public Integer getBlocked() {
        return blocked;
    }

    public void setBlocked(Integer blocked) {
        this.blocked = blocked;
    }

    public Long getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Long lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public Long getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
