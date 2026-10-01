package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 美食地图（F142）：一起吃过/想吃的店与菜，打卡留评。 */
@Data
@TableName("couple_food_note")
public class CoupleFoodNote {

    public static final String STATUS_WANT = "WANT";
    public static final String STATUS_EATEN = "EATEN";
    public static final int SHOP_MAX = 60;
    public static final int DISH_MAX = 60;
    public static final int COMMENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String shop;
    private String dish;
    private String status;
    private Integer rating;
    private String comment;
    private Long created;

    public static CoupleFoodNote of(String spaceId, String fromUser, String shop, String dish) {
        CoupleFoodNote row = new CoupleFoodNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.shop = shop;
        row.dish = dish;
        row.status = STATUS_WANT;
        row.created = System.currentTimeMillis();
        return row;
    }
}
