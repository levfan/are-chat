package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F358 写给低落的自己：一人同时一封在途（存在 SEALED 即 400，查询约束不用唯一函数索引）。
 * 开读只有本人能做：POST /self/read 或领能量补给时顺带置 READ，均不推送给对方。
 */
@Data
@TableName("couple_echo_self_letter")
public class CoupleEchoSelfLetter {

    public static final int CONTENT_MAX = 300;
    public static final String STATUS_SEALED = "SEALED";
    public static final String STATUS_READ = "READ";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleEchoSelfLetter of(String spaceId, String fromUser, String content) {
        CoupleEchoSelfLetter row = new CoupleEchoSelfLetter();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.status = STATUS_SEALED;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isSealed() {
        return STATUS_SEALED.equals(status);
    }
}
