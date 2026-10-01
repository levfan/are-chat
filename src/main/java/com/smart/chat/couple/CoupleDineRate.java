package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F212 吃过星评：吃完登记一笔，攒成我们的餐厅档案。 */
@Data
@TableName("couple_dine_rate")
public class CoupleDineRate {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String dish;
    private Integer stars;
    private String comment;
    private String fromUser;
    private Long created;

    public static CoupleDineRate of(String spaceId, String day, String dish, int stars, String comment, String fromUser) {
        CoupleDineRate row = new CoupleDineRate();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.dish = dish;
        row.stars = stars;
        row.comment = comment;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();

        return row;
    }
}
