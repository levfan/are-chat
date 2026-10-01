package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F226 周末慢生活：周五各提一件什么都不赶的小事，周日打卡回放。 */
@Data
@TableName("couple_cozy_slow")
public class CoupleCozySlow {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String thing;
    private String doneDay;
    private Long created;
    private Long updatedAt;

    public static CoupleCozySlow of(String spaceId, String week, String fromUser, String thing) {
        CoupleCozySlow row = new CoupleCozySlow();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.thing = thing;
        row.doneDay = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
