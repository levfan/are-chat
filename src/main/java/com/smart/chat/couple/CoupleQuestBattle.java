package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F370 关卡预告：把自己的大日子（面试/汇报/答辩/谈判/体检/其它）先挂给 TA。
 * 在途口径：status=PREP 的行按 fromUser 计数，每人同时 ≤3；对方报出战报（F371）后置 DONE。
 */
@Data
@TableName("couple_quest_battle")
public class CoupleQuestBattle {

    public static final String KIND_INTERVIEW = "INTERVIEW";
    public static final String KIND_REPORT = "REPORT";
    public static final String KIND_DEFEND = "DEFEND";
    public static final String KIND_TALK = "TALK";
    public static final String KIND_CHECKUP = "CHECKUP";
    public static final String KIND_OTHER = "OTHER";
    public static final List<String> KINDS =
            List.of(KIND_INTERVIEW, KIND_REPORT, KIND_DEFEND, KIND_TALK, KIND_CHECKUP, KIND_OTHER);
    public static final String STATUS_PREP = "PREP";
    public static final String STATUS_DONE = "DONE";
    /** 关卡名字数上限（列 varchar(90) 已按 3 倍宽度放宽） */
    public static final int NAME_MAX = 30;
    /** 怯场话字数上限（列 varchar(180) 已按 3 倍宽度放宽） */
    public static final int FEAR_MAX = 60;
    /** 在途（PREP）每人同时最多 3 关 */
    public static final int IN_FLIGHT_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 打这一关的人（区分大小写） */
    private String fromUser;
    /** 关卡日 yyyy-MM-dd（今天或以后） */
    private String day;
    /** INTERVIEW/REPORT/DEFEND/TALK/CHECKUP/OTHER（服务层校验） */
    private String kind;
    private String name;
    /** 一句怯场话；空串=没说 */
    private String fear;
    /** PREP 在途 / DONE 已报战报 */
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestBattle of(String spaceId, String fromUser, String day, String kind,
                                       String name, String fear) {
        CoupleQuestBattle row = new CoupleQuestBattle();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.kind = kind;
        row.name = name;
        row.fear = fear == null ? "" : fear;
        row.status = STATUS_PREP;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否还在途（还没报战报）。 */
    public boolean prep() {
        return STATUS_PREP.equals(status);
    }

    /** 战报已出，翻成 DONE（同时刷新更新时间）。 */
    public void done() {
        status = STATUS_DONE;
        updatedAt = System.currentTimeMillis();
    }
}
