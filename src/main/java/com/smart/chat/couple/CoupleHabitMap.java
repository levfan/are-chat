package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F287 习惯图鉴：观察 TA 的小习惯，TA 可标确实/冤枉。 */
@Data
@TableName("couple_habit_map")
public class CoupleHabitMap {

    public static final String VERDICT_REAL = "REAL";
    public static final String VERDICT_WRONG = "WRONG";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String habit;
    private String tag;
    private String observerUser;
    private String targetUser;
    private String verdict;
    private Long created;

    public static CoupleHabitMap of(String spaceId, String habit, String tag,
                                    String observerUser, String targetUser) {
        CoupleHabitMap row = new CoupleHabitMap();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.habit = habit;
        row.tag = tag == null ? "" : tag;
        row.observerUser = observerUser;
        row.targetUser = targetUser;
        row.verdict = "";
        row.created = System.currentTimeMillis();
        return row;
    }
}
