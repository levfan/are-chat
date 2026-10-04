package com.smart.chat.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * admin_audit 的表映射（业务名让给 {@code domain.audit.AdminAudit}，薄流水实体）。
 * append-only：只有插入与倒序读取，没有更新路径。
 */
@Data
@TableName("admin_audit")
public class AdminAuditPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String actor;
    private String action;
    private String target;
    private String detail;
    private Long created;
}
