package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F327 我错了榜：认错需一句具体说明，对方可点「最感人」。 */
@Data
@TableName("couple_admit_log")
public class CoupleAdmitLog {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String aboutUser;
    private String detail;
    private Integer touched;
    private Long created;

    public static CoupleAdmitLog of(String spaceId, String day, String fromUser, String aboutUser, String detail) {
        CoupleAdmitLog row = new CoupleAdmitLog();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.aboutUser = aboutUser;
        row.detail = detail;
        row.touched = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean touchedFlag() {
        return touched != null && touched == 1;
    }
}
