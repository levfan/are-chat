package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 见面日记（F118）：每次真实见面记一笔，能量瓶与异地恋报告的数据源。 */
@Data
@TableName("couple_reunion_log")
public class CoupleReunionLog {

    public static final int NOTE_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String byUser;
    private String meetDay;
    private String note;
    private Long created;

    public static CoupleReunionLog of(String spaceId, String byUser, String meetDay, String note) {
        CoupleReunionLog row = new CoupleReunionLog();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.byUser = byUser;
        row.meetDay = meetDay;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
