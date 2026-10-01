package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 下次见面信（F115）：写给「见面时」的信，见面打卡后才可拆。 */
@Data
@TableName("couple_reunion_letter")
public class CoupleReunionLetter {

    public static final String STATUS_SEELED = "SEELED";
    public static final String STATUS_OPENED = "OPENED";
    public static final int CONTENT_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String status;
    private Long openedAt;
    private Long created;

    public static CoupleReunionLetter of(String spaceId, String fromUser, String content) {
        CoupleReunionLetter row = new CoupleReunionLetter();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.status = STATUS_SEELED;
        row.created = System.currentTimeMillis();
        return row;
    }
}
