package com.smart.chat.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 88 全站公告：管理员发布，所有登录用户顶部横幅展示，关闭后不再展示。
 */
@TableName("announcement")
public class Announcement {

    @TableId(value = "ID", type = IdType.INPUT)
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
