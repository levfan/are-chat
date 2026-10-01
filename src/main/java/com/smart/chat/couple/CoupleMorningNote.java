package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 醒来第一条（F162）：睡前写留言，次日晨才送达对方，读后标记。 */
@Data
@TableName("couple_morning_note")
public class CoupleMorningNote {

    public static final int CONTENT_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String deliverDay;
    private Long readAt;
    private Long created;

    public static CoupleMorningNote of(String spaceId, String fromUser, String content, String deliverDay) {
        CoupleMorningNote row = new CoupleMorningNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.deliverDay = deliverDay;
        row.created = System.currentTimeMillis();
        return row;
    }
}
