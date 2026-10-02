package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F368 数字排毒半天：AM/PM 二选一发起半日无手机挑战（day 一天一格），
 * 双方各报各的（双人列口径同 F360），双报=达成「清净半天」。
 */
@Data
@TableName("couple_focus_detox")
public class CoupleFocusDetox {

    public static final String KIND_AM = "AM";
    public static final String KIND_PM = "PM";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    /** AM=上半天 / PM=下半天（只此两种） */
    private String kind;
    /** 1=userA 应战 */
    private Integer tickA;
    /** 1=userB 应战 */
    private Integer tickB;
    /** 最先应战的人；null=还没人应战 */
    private String confirmedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleFocusDetox of(String spaceId, String day, String kind) {
        CoupleFocusDetox row = new CoupleFocusDetox();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.kind = kind;
        row.tickA = 0;
        row.tickB = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean ticked(boolean isA) {
        Integer v = isA ? tickA : tickB;
        return v != null && v == 1;
    }

    /** 双方都报=达成清净半天。 */
    public boolean bothTicked() {
        return ticked(true) && ticked(false);
    }

    /** 应战：第一次应战的人记进 confirmed_by；返回本次是否真的从 0 变 1。 */
    public boolean tick(boolean isA, String user) {
        if (confirmedBy == null || confirmedBy.isBlank()) {
            confirmedBy = user;
        }
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
