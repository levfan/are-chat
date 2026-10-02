package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F377 小胜利账本：每天记一件做成的小事（uk(space_id,day,from_user) 每人每天一条），
 * 周日由对方互颁「小赢奖」。
 * 获奖口径：award_day 非空即已获奖（空=没获奖），awarded_by 记颁奖的人（只能对方颁）。
 */
@Data
@TableName("couple_quest_win")
public class CoupleQuestWin {

    /** 做成的小事字数上限（列 varchar(120) 已按 3 倍宽度放宽） */
    public static final int CONTENT_MAX = 40;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 做成小事的那天 yyyy-MM-dd */
    private String day;
    /** 记账的人（区分大小写） */
    private String fromUser;
    /** 做成的小事 */
    private String content;
    /** 被对方评为本周最佳的日子 yyyy-MM-dd；空串=没获奖 */
    private String awardDay;
    /** 颁奖的人（只能对方颁）；空串=没人颁 */
    private String awardedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestWin of(String spaceId, String day, String fromUser, String content) {
        CoupleQuestWin row = new CoupleQuestWin();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.content = content;
        row.awardDay = "";
        row.awardedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否已被颁过「小赢奖」（award_day 非空非空白）。 */
    public boolean awarded() {
        return awardDay != null && !awardDay.isBlank();
    }

    /** 颁奖：写颁奖人与获奖日（同时刷新更新时间）。 */
    public void award(String by, String day) {
        awardedBy = by;
        awardDay = day;
        updatedAt = System.currentTimeMillis();
    }
}
