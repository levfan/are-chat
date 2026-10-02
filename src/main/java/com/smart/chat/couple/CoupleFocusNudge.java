package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F366 走神温柔哨：一天 2 张额度递「回来啦」卡，防唠叨限流（超出 400）。
 * 用 from_user 单列（每人每天独立额度），note 可空 ≤40 字。
 */
@Data
@TableName("couple_focus_nudge")
public class CoupleFocusNudge {

    public static final int DAILY_MAX = 2;
    public static final int NOTE_MAX = 40;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    /** 递卡的人（每人每天 ≤2 张） */
    private String fromUser;
    private String note;
    private Long created;

    public static CoupleFocusNudge of(String spaceId, String day, String fromUser, String note) {
        CoupleFocusNudge row = new CoupleFocusNudge();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
