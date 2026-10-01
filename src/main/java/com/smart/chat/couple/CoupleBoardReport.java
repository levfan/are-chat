package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F242 年度股东大会：本年述职+明年一个小目标，双提交互见。 */
@Data
@TableName("couple_board_report")
public class CoupleBoardReport {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String review;
    private String goal;
    private Long created;
    private Long updatedAt;

    public static CoupleBoardReport of(String spaceId, String year, String fromUser, String review, String goal) {
        CoupleBoardReport row = new CoupleBoardReport();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.review = review;
        row.goal = goal;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
