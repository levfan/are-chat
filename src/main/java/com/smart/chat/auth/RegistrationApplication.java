package com.smart.chat.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 77 注册申请：新用户提交手机号/用户名/密码后先进申请表，
 * 管理员审批通过才真正创建 app_user（审批流之前不产生任何合法账号）。
 * 密码以 PBKDF2 哈希保存在申请行里，审批通过时原样搬入 app_user。
 */
@TableName("registration_application")
public class RegistrationApplication {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;
    private String phone;
    private String username;
    /** 注册时自愿填写的昵称（选填，审批通过后写入 app_user / user_profile） */
    private String nickname;
    private String passwordHash;
    private String status;
    private String rejectReason;
    private Long created;
    private Long reviewedAt;
    private String reviewedBy;

    public static RegistrationApplication of(String phone, String username, String passwordHash) {
        return of(phone, username, null, passwordHash);
    }

    public static RegistrationApplication of(String phone, String username, String nickname, String passwordHash) {
        RegistrationApplication application = new RegistrationApplication();
        application.id = UUID.randomUUID().toString();
        application.phone = phone;
        application.username = username;
        application.nickname = nickname;
        application.passwordHash = passwordHash;
        application.status = STATUS_PENDING;
        application.created = System.currentTimeMillis();
        return application;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Long reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
