package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 感恩便签墙（F151）：每天一句感谢对方的小事。 */
@Data
@TableName("couple_thanks_note")
public class CoupleThanksNote {

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private Long created;

    public static CoupleThanksNote of(String spaceId, String fromUser, String content) {
        CoupleThanksNote row = new CoupleThanksNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
