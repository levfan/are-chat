package com.smart.chat.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 系统合法用户：手机号注册产生，手机号与用户名均唯一。
 * 昵称/头像/签名/在线状态仍存 user_profile（注册时同步建一行），保持既有资料逻辑不变。
 */
@Data
@TableName("app_user")
public class AppUser {

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
    private String signature;
    private String presenceStatus;
    private String status;
    /** USER / ADMIN（管理员可审批注册申请、管理用户与公告） */
    private String role;
    private Long created;
    private Long lastLoginAt;

    public static AppUser of(String phone, String username, String passwordHash, String nickname, String avatar) {
        return of(phone, username, passwordHash, nickname, avatar, ROLE_USER);
    }

    public static AppUser of(String phone, String username, String passwordHash, String nickname, String avatar, String role) {
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
        user.role = role == null || role.isBlank() ? ROLE_USER : role;
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

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
