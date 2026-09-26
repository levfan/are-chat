package com.smart.chat.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 88 全站公告：管理员发布，所有登录用户顶部横幅展示，关闭后不再展示。
 */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String content;
    private String createdBy;
    private Boolean enabled;
    private Long created;

    public static Announcement of(String content, String createdBy) {
        Announcement announcement = new Announcement();
        announcement.id = UUID.randomUUID().toString();
        announcement.content = content;
        announcement.createdBy = createdBy;
        announcement.enabled = true;
        announcement.created = System.currentTimeMillis();
        return announcement;
    }
}
