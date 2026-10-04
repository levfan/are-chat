package com.smart.chat.messaging.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 好友关系（双向两行：owner=拥有者，friend=好友）。
 * lastReadAt 用于未读红点计算；pinned 置顶；muted 免打扰；lastSeenAt 最近在线时间。
 */
@Data
@TableName("friend")
public class FriendPO {

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

    public static FriendPO of(String owner, String friend) {
        FriendPO row = new FriendPO();
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
}
