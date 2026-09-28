package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 生理期记录：每人记自己的最近一次生理期开始日期 + 周期/经期长度，
 * 系统自动推算下一次日期与「特殊时期」区间，对方端展示温柔模式提醒。
 */
@Data
@TableName("couple_cycle")
public class CoupleCycle {

    public static final int DEFAULT_CYCLE_DAYS = 28;
    public static final int DEFAULT_PERIOD_DAYS = 5;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 记录人用户名（记自己的） */
    private String username;
    /** 最近一次生理期开始日期（yyyy-MM-dd） */
    private String periodDay;
    /** 周期长度（天） */
    private Integer cycleDays;
    /** 经期持续天数 */
    private Integer periodDays;
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleCycle of(String spaceId, String username, String periodDay,
                                 Integer cycleDays, Integer periodDays, String note) {
        CoupleCycle row = new CoupleCycle();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.username = username;
        row.periodDay = periodDay;
        row.cycleDays = cycleDays == null ? DEFAULT_CYCLE_DAYS : cycleDays;
        row.periodDays = periodDays == null ? DEFAULT_PERIOD_DAYS : periodDays;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }

    /** 某天是否落在经期区间内（从最近一次开始日按周期外推）。 */
    public boolean inPeriod(LocalDate day) {
        LocalDate start;
        try {
            start = LocalDate.parse(periodDay);
        } catch (Exception e) {
            return false;
        }
        int cycle = cycleDays == null || cycleDays <= 0 ? DEFAULT_CYCLE_DAYS : cycleDays;
        int period = periodDays == null || periodDays <= 0 ? DEFAULT_PERIOD_DAYS : periodDays;
        long diff = java.time.temporal.ChronoUnit.DAYS.between(start, day);
        if (diff < 0) {
            return false;
        }
        return diff % cycle < period;
    }

    /** 下一次生理期开始日期（从最近一次按周期外推到今天及以后）。 */
    public LocalDate nextStart(LocalDate today) {
        LocalDate start;
        try {
            start = LocalDate.parse(periodDay);
        } catch (Exception e) {
            return null;
        }
        int cycle = cycleDays == null || cycleDays <= 0 ? DEFAULT_CYCLE_DAYS : cycleDays;
        LocalDate cursor = start;
        while (cursor.isBefore(today)) {
            cursor = cursor.plusDays(cycle);
        }
        return cursor;
    }
}
