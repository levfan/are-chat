package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 倒数日期待清单：把期待的事写下来（约会/旅行/惊喜），倒数展示，
 * 临近 7/3/1/0 天自动提醒双方，实现后归档进回忆。
 */
@Data
@TableName("couple_countdown")
public class CoupleCountdown {

    public static final int TITLE_MAX = 60;
    public static final int NOTE_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 目标日期（yyyy-MM-dd） */
    private String targetDay;
    private String note;
    private Integer done;
    private Long doneAt;
    /** 最近一次倒数提醒日期（yyyy-MM-dd，防重复打扰） */
    private String lastRemindDay;
    private String createdBy;
    private Long created;

    public static CoupleCountdown of(String spaceId, String createdBy, String title, String targetDay, String note) {
        CoupleCountdown row = new CoupleCountdown();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.createdBy = createdBy;
        row.title = title;
        row.targetDay = targetDay;
        row.note = note;
        row.done = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean doneFlag() {
        return done != null && done == 1;
    }

    /** 距目标日期的天数（已过期返回负数）。 */
    public long daysLeft(LocalDate today) {
        try {
            return java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(targetDay));
        } catch (Exception e) {
            return 0;
        }
    }
}
