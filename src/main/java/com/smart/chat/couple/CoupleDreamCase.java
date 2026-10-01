package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F297 解梦局：记梦投稿，对方一本正经点评，做梦人判「解得灵/胡说」。 */
@Data
@TableName("couple_dream_case")
public class CoupleDreamCase {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String dreamerUser;
    private String dream;
    private String reading;
    private String readBy;
    private Integer good;
    private Long created;
    private Long updatedAt;

    public static CoupleDreamCase of(String spaceId, String day, String dreamerUser, String dream) {
        CoupleDreamCase row = new CoupleDreamCase();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.dreamerUser = dreamerUser;
        row.dream = dream;
        row.reading = "";
        row.readBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
