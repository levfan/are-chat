package com.smart.chat.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * app_user 的表映射（ADR-0002：{@code *PO} 只做映射，业务名「账号」让给 {@code domain.account.Account}）。
 * <p>
 * 表名、列名、{@code @TableName} 值与 {@code @TableId(IdType.INPUT)} 原样未动，数据库与 Flyway 无感。
 * 状态/角色常量留在这里供 Mapper 的 default 查询使用；业务判定（能不能登录、是不是管理员、脱敏）
 * 一律在聚合里，<b>PO 上不许长方法</b>。
 */
@Data
@TableName("app_user")
public class AppUserPO {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
    /** 84 账号自助注销：保留用户名/手机号占位，禁止登录、不可被搜索/加好友 */
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String username;
    private String phone;
    private String nickname;
    private String passwordHash;
    private String avatar;
    /** 个性签名：权威副本在 user_profile（归 messaging），这里只是历史快照，identity 不维护 */
    private String signature;
    /** 在线状态：同上，实时状态以 user_profile 为准 */
    private String presenceStatus;
    private String status;
    /** USER / ADMIN（管理员可审批注册申请、管理用户与公告） */
    private String role;
    private Long created;
    private Long lastLoginAt;
}
