package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F372 加班预报：今晚忙到几点先说一声（uk(space_id,day,from_user) 每人每天一行），
 * 对方可回一张「到家灯给你留着」卡。
 * 留灯口径：lamp_by 非空即已留（NULL=没留），lamp 空串=还没写灯卡文字。
 */
@Data
@TableName("couple_quest_overtime")
public class CoupleQuestOvertime {

    public static final int HOUR_MIN = 13;
    public static final int HOUR_MAX = 23;
    public static final int HOUR_DEFAULT = 20;
    /** 一句说明字数上限（列 varchar(120) 已按 3 倍宽度放宽） */
    public static final int NOTE_MAX = 40;
    /** 灯卡字数上限（列 varchar(180) 已按 3 倍宽度放宽） */
    public static final int LAMP_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 预报日 yyyy-MM-dd */
    private String day;
    /** 加班的人（区分大小写） */
    private String fromUser;
    /** 预计忙到几点 13-23（服务层钳制） */
    private Integer untilHour;
    /** 一句说明；空串=没写 */
    private String note;
    /** 对方留的灯卡；空串=还没留 */
    private String lamp;
    /** 留灯的人；null=没留 */
    private String lampBy;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestOvertime of(String spaceId, String day, String fromUser, int untilHour, String note) {
        CoupleQuestOvertime row = new CoupleQuestOvertime();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.untilHour = untilHour;
        row.note = note;
        row.lamp = "";
        row.lampBy = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 对方是否已留灯（lamp_by 非空非空白）。 */
    public boolean lampLeft() {
        return lampBy != null && !lampBy.isBlank();
    }

    /** 留一张灯卡：写灯卡文字与留灯人，并刷新更新时间。 */
    public void leaveLamp(String by, String text) {
        lamp = text;
        lampBy = by;
        updatedAt = System.currentTimeMillis();
    }
}
