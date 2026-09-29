package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 双人习惯打卡日志：每人每天一条（习惯+人+自然日唯一）。 */
@Data
@TableName("couple_habit_log")
public class CoupleHabitLog {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String habitId;
    private String username;
    /** 打卡日期（yyyy-MM-dd） */
    private String logDay;
    private Long created;

    public static CoupleHabitLog of(String habitId, String username, String logDay) {
        CoupleHabitLog row = new CoupleHabitLog();
        row.id = UUID.randomUUID().toString();
        row.habitId = habitId;
        row.username = username;
        row.logDay = logDay;
        row.created = System.currentTimeMillis();
        return row;
    }
}
