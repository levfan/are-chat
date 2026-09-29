package com.smart.chat.im;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户资料：头像色档 + 昵称 + 个性签名 + 在线状态（online/busy/away）。
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(value = "username", type = IdType.INPUT)
    private String username;
    private String nickname;
    private String signature;
    private String avatar;
    private String presenceStatus;
    /** F42 生日（yyyy-MM-dd 或 MM-dd，空 = 未填写） */
    private String birthday;
    private Long updatedAt;
}
