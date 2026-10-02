package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F375 新家第一晚：搬进新家的第一晚双人才算庆祝打卡。
 * 双人列口径同 F360/F363：tick_a 属 CoupleSpace.userA，tick_b 属 userB，
 * 服务层按 mine = (me == userA) 读自己那一列，两点齐当夜才算点亮。
 */
@Data
@TableName("couple_quest_move_night")
public class CoupleQuestMoveNight {

    /** 那一晚一句话字数上限（列 varchar(180) 已按 3 倍宽度放宽） */
    public static final int NOTE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 第一晚是哪天 yyyy-MM-dd */
    private String day;
    /** 1=userA 点过 */
    private Integer tickA;
    /** 1=userB 点过 */
    private Integer tickB;
    /** 那一晚的一句话；空串=没写 */
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestMoveNight of(String spaceId, String day) {
        CoupleQuestMoveNight row = new CoupleQuestMoveNight();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.tickA = 0;
        row.tickB = 0;
        row.note = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 某一方是否点过（isA=true 读 A 列）。 */
    public boolean ticked(boolean isA) {
        Integer v = isA ? tickA : tickB;
        return v != null && v == 1;
    }

    /** 双点=这一晚庆祝成立。 */
    public boolean bothTicked() {
        return ticked(true) && ticked(false);
    }

    /** 点自己那一格；返回本次是否真的从 0 变 1（已点过不改写，用于只推一次）。 */
    public boolean tick(boolean isA) {
        if (ticked(isA)) {
            return false;
        }
        if (isA) {
            tickA = 1;
        } else {
            tickB = 1;
        }
        updatedAt = System.currentTimeMillis();
        return true;
    }
}
