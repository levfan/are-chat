package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 打卡日：一行代表「这一天双方都答完了每日一问」（或空间建立当天、或事后补签成功）。
 * 连续天数与七档解锁全部由本表的 day 集合读时算出，不另存缓存列。
 */
@Data
@TableName("couple_streak_day")
public class CoupleStreakDayPO {

    /** 双方当天都答完每日一问（或空间建立当天），系统自动确认 */
    public static final String SOURCE_AUTO = "AUTO";
    /** 补签成功（免费） */
    public static final String SOURCE_MAKEUP = "MAKEUP";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** yyyy-MM-dd */
    private String day;
    private String source;
    /** 补签操作人；AUTO 时为 null */
    private String operatorUser;
    private Long created;

    public static CoupleStreakDayPO of(String spaceId, String day, String source, String operatorUser) {
        CoupleStreakDayPO row = new CoupleStreakDayPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.source = source;
        row.operatorUser = operatorUser;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean makeupFlag() {
        return SOURCE_MAKEUP.equals(source);
    }
}
