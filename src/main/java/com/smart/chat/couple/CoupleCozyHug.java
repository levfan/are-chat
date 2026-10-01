package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F228 抱抱计量器：见面拥抱自报计数，累计里程碑点亮（呼应 F60）。 */
@Data
@TableName("couple_cozy_hug")
public class CoupleCozyHug {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer cnt;
    private String note;
    private Long created;

    public static CoupleCozyHug of(String spaceId, String day, String fromUser, int cnt, String note) {
        CoupleCozyHug row = new CoupleCozyHug();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.cnt = cnt;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
