package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F266 三行打卡：今日印象/谢一件/夸一件，一人一天一条可改写。 */
@Data
@TableName("couple_three_line")
public class CoupleThreeLine {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String morning;
    private String thanks;
    private String praise;
    private Long created;
    private Long updatedAt;

    public static CoupleThreeLine of(String spaceId, String day, String fromUser,
                                     String morning, String thanks, String praise) {
        CoupleThreeLine row = new CoupleThreeLine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.morning = morning == null ? "" : morning;
        row.thanks = thanks == null ? "" : thanks;
        row.praise = praise == null ? "" : praise;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
