package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 双人习惯：一起坚持一件小事，每天双方打卡，断签互相提醒。 */
@Data
@TableName("couple_habit")
public class CoupleHabit {

    public static final int TITLE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private String createdBy;
    private Integer active;
    private Long created;

    public static CoupleHabit of(String spaceId, String createdBy, String title) {
        CoupleHabit row = new CoupleHabit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.createdBy = createdBy;
        row.title = title;
        row.active = 1;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean activeFlag() {
        return active != null && active == 1;
    }
}
