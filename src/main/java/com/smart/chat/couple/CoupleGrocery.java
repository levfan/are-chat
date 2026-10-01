package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F278 逛超市战利品：一周一报，对方猜为什么买，报的人打分。 */
@Data
@TableName("couple_grocery")
public class CoupleGrocery {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String items;
    private String guess;
    private String guessBy;
    private Integer score;
    private Long created;
    private Long updatedAt;

    public static CoupleGrocery of(String spaceId, String week, String fromUser, String items) {
        CoupleGrocery row = new CoupleGrocery();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.items = items;
        row.guess = "";
        row.guessBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
