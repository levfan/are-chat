package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情侣空间通知中心：每次事件推送都给收件人存档一条，登录后可补看。 */
@Data
@TableName("couple_notify")
public class CoupleNotifyPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String username;
    private String event;
    /** 触发人（system = 定时任务） */
    private String actor;
    private String detail;
    private Integer readFlag;
    private Long created;

    public static CoupleNotifyPO of(String username, String event, String actor, String detail) {
        CoupleNotifyPO row = new CoupleNotifyPO();
        row.id = UUID.randomUUID().toString();
        row.username = username;
        row.event = event;
        row.actor = actor;
        row.detail = detail == null ? "" : detail;
        row.readFlag = 0;
        row.created = System.currentTimeMillis();
        return row;
    }
}
