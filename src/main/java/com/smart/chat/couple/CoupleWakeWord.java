package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F274 叫醒服务：一周定一句叫醒词，另一人每天可递一张叫醒卡。 */
@Data
@TableName("couple_wake_word")
public class CoupleWakeWord {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String content;
    private String givenDay;
    private String givenBy;
    private Long created;

    public static CoupleWakeWord of(String spaceId, String week, String fromUser, String content) {
        CoupleWakeWord row = new CoupleWakeWord();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.content = content;
        row.givenDay = "";
        row.givenBy = "";
        row.created = System.currentTimeMillis();
        return row;
    }
}
