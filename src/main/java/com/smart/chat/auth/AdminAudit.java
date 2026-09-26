package com.smart.chat.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 93 管理员操作审计：审批 / 禁用启用 / 重置密码 / 发公告等敏感动作全部留痕。
 */
@Data
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
}
