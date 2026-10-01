package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F290 五年后新年卡：每人每年一张，到期读时放行。 */
@Data
@TableName("couple_post_oath")
public class CouplePostOath {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String content;
    private String deliverDay;
    private String status;
    private Long sentAt;
    private Long created;

    public static CouplePostOath of(String spaceId, String year, String fromUser, String content, String deliverDay) {
        CouplePostOath row = new CouplePostOath();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.content = content;
        row.deliverDay = deliverDay;
        row.status = "SEALED";
        row.created = System.currentTimeMillis();
        return row;
    }
}
