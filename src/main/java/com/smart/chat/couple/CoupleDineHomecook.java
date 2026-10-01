package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F215 家常菜搭档：每周各报一道拿手菜+自封配饭指数。 */
@Data
@TableName("couple_dine_homecook")
public class CoupleDineHomecook {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String dish;
    private Integer score;
    private Long created;

    public static CoupleDineHomecook of(String spaceId, String week, String fromUser, String dish, int score) {
        CoupleDineHomecook row = new CoupleDineHomecook();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.dish = dish;
        row.score = score;
        row.created = System.currentTimeMillis();

        return row;
    }
}
