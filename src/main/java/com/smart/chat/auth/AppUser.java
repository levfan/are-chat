package com.smart.chat.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 系统合法用户：手机号注册产生，手机号与用户名均唯一。
 * 昵称/头像/签名/在线状态仍存 user_profile（注册时同步建一行），保持既有资料逻辑不变。
 */
@TableName("app_user")
public class AppUser {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    @TableId(value = "ID", type = IdType.INPUT)
    private String id;
    private String username;
    private String phone;
    private String nickname;
    private String passwordHash;
    private String avatar;
    private String signature;
    private String presenceStatus;
    private String status;
    private Long created;
    private Long lastLoginAt;

    public static AppUser of(String phone, String username, String passwordHash, String nickname, String avatar) {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID().toString();
        user.phone = phone;
        user.username = username;
        user.nickname = nickname == null || nickname.isBlank() ? username : nickname;
        user.passwordHash = passwordHash;
        user.avatar = avatar == null || avatar.isBlank() ? "c0" : avatar;
        user.signature = "";
        user.presenceStatus = "online";
        user.status = STATUS_ACTIVE;
        user.created = System.currentTimeMillis();
        return user;
    }

    /** 手机号脱敏：138****0001 */
    public String maskedPhone() {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPresenceStatus() {
        return presenceStatus;
    }

    public void setPresenceStatus(String presenceStatus) {
        this.presenceStatus = presenceStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
