package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 安全感账户（F120）：TA 说一句让你安心的话，你收进账户。 */
@Data
@TableName("couple_security_bank")
public class CoupleSecurityBank {

    public static final String STATUS_DEPOSITED = "DEPOSITED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String status;
    private Long acceptedAt;
    private Long created;

    public static CoupleSecurityBank of(String spaceId, String fromUser, String content) {
        CoupleSecurityBank row = new CoupleSecurityBank();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.status = STATUS_DEPOSITED;
        row.created = System.currentTimeMillis();
        return row;
    }
}
