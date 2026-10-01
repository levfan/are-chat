package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 共同日历纪念日：纪念日、生日、约会日。yearly=1 表示每年重复（生日/周年），
 * yearly=0 表示仅当年某天的一次性约会安排。
 */
@Data
@TableName("couple_anniversary")
public class CoupleAnniversary {

    public static final int TITLE_MAX = 60;
    /** F127 大日子类型：NORMAL 普通 / LOVE 恋爱 / FAMILY 家人 / FRIEND 朋友 / WORK 工作 */
    public static final String KIND_NORMAL = "NORMAL";
    public static final String KIND_LOVE = "LOVE";
    public static final String KIND_FAMILY = "FAMILY";
    public static final String KIND_FRIEND = "FRIEND";
    public static final String KIND_WORK = "WORK";
    /** F253 历法：SOLAR 公历 / LUNAR 农历 */
    public static final String CALENDAR_SOLAR = "SOLAR";
    public static final String CALENDAR_LUNAR = "LUNAR";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 日期 yyyy-MM-dd */
    private String eventDate;
    private Integer yearly;
    /** 日子类型（F127），默认 NORMAL */
    private String kind;
    /** F253 历法类型：SOLAR 公历 / LUNAR 农历（农历时 eventDate 仅为首年换算结果，lunarMd 为真源） */
    private String calendarType;
    /** F253 农历月日 MMDD（LUNAR 时有效） */
    private String lunarMd;
    private String createdBy;
    private Long created;

    public static CoupleAnniversary of(String spaceId, String title, String date, boolean yearly, String createdBy) {
        CoupleAnniversary row = new CoupleAnniversary();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.eventDate = date;
        row.yearly = yearly ? 1 : 0;
        row.kind = KIND_NORMAL;
        row.calendarType = CALENDAR_SOLAR;
        row.lunarMd = "";
        row.createdBy = createdBy;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isYearly() {
        return Integer.valueOf(1).equals(yearly);
    }
}
