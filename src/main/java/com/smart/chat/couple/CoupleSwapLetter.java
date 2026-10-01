package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F264 换位信：以对方的口吻写信封存，到开放日互拆。 */
@Data
@TableName("couple_swap_letter")
public class CoupleSwapLetter {

    public static final String STATUS_SEALED = "SEALED";
    public static final String STATUS_OPENED = "OPENED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String content;
    private String openDay;
    private String status;
    private Long openedAt;
    private Long created;

    public static CoupleSwapLetter of(String spaceId, String day, String fromUser, String content, String openDay) {
        CoupleSwapLetter row = new CoupleSwapLetter();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.content = content;
        row.openDay = openDay;
        row.status = STATUS_SEALED;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isOpenable(String today) {
        return STATUS_SEALED.equals(status) && openDay != null && openDay.compareTo(today) <= 0;
    }
}
