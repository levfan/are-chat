package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F257 长假愿望：内置法定假日倒数，一人首写、另一人可补写一段。 */
@Data
@TableName("couple_holiday_wish")
public class CoupleHolidayWish {

    public static final int WISH_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String holiday;
    private String day;
    private String wish;
    private String wishedBy;
    private String appendedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleHolidayWish of(String spaceId, String holiday, String day, String fromUser, String wish) {
        CoupleHolidayWish row = new CoupleHolidayWish();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.holiday = holiday;
        row.day = day;
        row.wish = wish;
        row.wishedBy = fromUser;
        row.appendedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean hasWish() {
        return wishedBy != null && !wishedBy.isEmpty();
    }

    public boolean hasAppend() {
        return appendedBy != null && !appendedBy.isEmpty();
    }
}
