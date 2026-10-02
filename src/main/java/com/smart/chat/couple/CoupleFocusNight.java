package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F360 专注打卡：每晚自报「今晚放下手机陪了 TA 多少分钟」（0-180 钳制）。
 * 双人列口径：minutes_a / note_a 属 CoupleSpace.userA，_b 属 userB；
 * 双方都报（两列都非空）当夜才算点亮，bothLit 在读时算。
 */
@Data
@TableName("couple_focus_night")
public class CoupleFocusNight {

    public static final int MINUTES_MIN = 0;
    public static final int MINUTES_MAX = 180;
    public static final int NOTE_MAX = 40;
    public static final int DEED_PAGE = 30;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 打卡日 yyyy-MM-dd */
    private String day;
    /** userA 报的专注分钟；null=还没报 */
    private Integer minutesA;
    /** userB 报的专注分钟；null=还没报 */
    private Integer minutesB;
    private String noteA;
    private String noteB;
    private Long created;
    private Long updatedAt;

    public static CoupleFocusNight of(String spaceId, String day) {
        CoupleFocusNight row = new CoupleFocusNight();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.noteA = "";
        row.noteB = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 某一方是否已报（isA=true 读 A 列）。 */
    public boolean reported(boolean isA) {
        return (isA ? minutesA : minutesB) != null;
    }

    /** 双方都报当夜点亮。 */
    public boolean bothLit() {
        return minutesA != null && minutesB != null;
    }

    /** 取某一方报的分钟（没报给 0）。 */
    public int minutesOf(boolean isA) {
        Integer v = isA ? minutesA : minutesB;
        return v == null ? 0 : v;
    }

    /** 取某一方的一句话（没写给空串）。 */
    public String noteOf(boolean isA) {
        String v = isA ? noteA : noteB;
        return v == null ? "" : v;
    }

    /** 写入某一方那一列（A/B 列口径唯一入口）。 */
    public void report(boolean isA, int minutes, String note) {
        if (isA) {
            minutesA = minutes;
            noteA = note;
        } else {
            minutesB = minutes;
            noteB = note;
        }
        updatedAt = System.currentTimeMillis();
    }

    public long totalMinutes() {
        return minutesOf(true) + minutesOf(false);
    }
}
