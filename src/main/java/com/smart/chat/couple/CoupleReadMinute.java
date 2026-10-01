package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 共读一分钟（F154）：每日一段小文共读+各自一句感想。 */
@Data
@TableName("couple_read_minute")
public class CoupleReadMinute {

    public static final int THOUGHT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String thought;
    private Long created;

    public static CoupleReadMinute of(String spaceId, String day, String fromUser, String thought) {
        CoupleReadMinute row = new CoupleReadMinute();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.thought = thought;
        row.created = System.currentTimeMillis();
        return row;
    }
}
