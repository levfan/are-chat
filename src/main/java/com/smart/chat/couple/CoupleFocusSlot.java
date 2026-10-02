package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F361 专属时段：每周预约一段「只属于我们」的时光（写做什么 + 几小时）。
 * 提议人 proposed_by 不能给自己确认，必须对方 confirmer 点头才算生效；
 * day 必须落在该周（week = 那周周一）之内。
 */
@Data
@TableName("couple_focus_slot")
public class CoupleFocusSlot {

    public static final int HOURS_MIN = 1;
    public static final int HOURS_MAX = 6;
    public static final int HOURS_DEFAULT = 2;
    public static final int TITLE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 周锚=该周周一 yyyy-MM-dd */
    private String week;
    /** 时段落在周内哪一天 yyyy-MM-dd */
    private String day;
    private String title;
    private Integer hours;
    /** 提议人 */
    private String proposedBy;
    /** 确认人=对方；null=还没确认 */
    private String confirmer;
    private Long created;
    private Long updatedAt;

    public static CoupleFocusSlot of(String spaceId, String week, String day, String title, int hours,
                                     String proposedBy) {
        CoupleFocusSlot row = new CoupleFocusSlot();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.day = day;
        row.title = title;
        row.hours = hours;
        row.proposedBy = proposedBy;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean confirmed() {
        return confirmer != null && !confirmer.isBlank();
    }

    public int hoursOrDefault() {
        return hours == null ? HOURS_DEFAULT : hours;
    }
}
