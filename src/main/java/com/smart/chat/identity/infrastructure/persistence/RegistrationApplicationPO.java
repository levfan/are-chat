package com.smart.chat.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * registration_application 的表映射（业务名「注册申请」让给
 * {@code domain.registration.RegistrationApplication}）。
 * 表名/列名/{@code @TableName} 值/状态字面量原样未动。
 */
@Data
@TableName("registration_application")
public class RegistrationApplicationPO {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String phone;
    private String username;
    /** 注册时填写的昵称（必填，审批通过后写入 app_user / user_profile；历史申请可能为空） */
    private String nickname;
    private String passwordHash;
    private String status;
    private String rejectReason;
    private Long created;
    private Long reviewedAt;
    private String reviewedBy;
}
