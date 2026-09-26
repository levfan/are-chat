package com.smart.chat.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 93 管理员操作审计：审批 / 禁用启用 / 重置密码 / 发公告等敏感动作全部留痕。
 */
@TableName("admin_audit")
public class AdminAudit {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String actor;
    private String action;
    private String target;
    private String detail;
    private Long created;

    public static AdminAudit of(String actor, String action, String target, String detail) {
        AdminAudit audit = new AdminAudit();
        audit.id = UUID.randomUUID().toString();
        audit.actor = actor;
        audit.action = action;
        audit.target = target == null ? "" : target;
        audit.detail = detail == null ? "" : detail;
        audit.created = System.currentTimeMillis();
        return audit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
