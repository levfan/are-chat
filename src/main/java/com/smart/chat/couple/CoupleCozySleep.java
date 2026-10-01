package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F221 睡眠报告单：晨间各报昨夜自评（睡龄 1-5+一句梦话），互见。 */
@Data
@TableName("couple_cozy_sleep")
public class CoupleCozySleep {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer stars;
    private String dream;
    private Long created;
    private Long updatedAt;

    public static CoupleCozySleep of(String spaceId, String day, String fromUser, int stars, String dream) {
        CoupleCozySleep row = new CoupleCozySleep();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.stars = stars;
        row.dream = dream == null ? "" : dream;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
