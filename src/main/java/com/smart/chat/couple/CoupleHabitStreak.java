package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 21天习惯搭子（F150）：各自立习惯每日打卡，满 target 天自动达成。表名避开 V10 既有 couple_habit。 */
@Data
@TableName("couple_habit_streak")
public class CoupleHabitStreak {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    public static final int TITLE_MAX = 60;
    public static final int TARGET_MIN = 3;
    public static final int TARGET_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String title;
    private Integer targetDays;
    private Integer doneDays;
    private String lastDoneDay;
    private String status;
    private Long doneAt;
    private Long created;

    public static CoupleHabitStreak of(String spaceId, String fromUser, String title, int targetDays) {
        CoupleHabitStreak row = new CoupleHabitStreak();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.targetDays = targetDays;
        row.doneDays = 0;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
