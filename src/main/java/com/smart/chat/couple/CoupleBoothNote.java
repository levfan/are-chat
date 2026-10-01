package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F303 时空电话亭：给一年前/后的 TA 留言，读时到点接通。 */
@Data
@TableName("couple_booth_note")
public class CoupleBoothNote {

    public static final String KIND_FUTURE = "FUTURE";
    public static final String KIND_PAST = "PAST";
    public static final String STATUS_SEALED = "SEALED";
    public static final String STATUS_SENT = "SENT";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String kind;
    private String text;
    private String openDay;
    private String status;
    private Long sentAt;
    private Long created;

    public static CoupleBoothNote of(String spaceId, String fromUser, String kind, String text, String openDay) {
        CoupleBoothNote row = new CoupleBoothNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.kind = kind;
        row.text = text;
        row.openDay = openDay;
        row.status = STATUS_SEALED;
        row.created = System.currentTimeMillis();
        return row;
    }
}
