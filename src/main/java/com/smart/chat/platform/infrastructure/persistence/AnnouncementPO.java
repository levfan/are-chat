package com.smart.chat.platform.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 88 全站公告的**持久化模型**（ADR-0002：业务名 {@code Announcement} 让给 domain，PO 只做表映射）。
 * 表名、列名、主键与 TableName 注解口径原样保留，数据库与 Flyway 无感。
 */
@Data
@TableName("announcement")
public class AnnouncementPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String content;
    private String createdBy;
    private Boolean enabled;
    private Long created;
}
