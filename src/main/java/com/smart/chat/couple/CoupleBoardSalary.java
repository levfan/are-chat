package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F244 发薪日：每月各发一句「本月感谢工资」，另 +5 积分入账台账。 */
@Data
@TableName("couple_board_salary")
public class CoupleBoardSalary {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String month;
    private String fromUser;
    private String thanks;
    private Long created;

    public static CoupleBoardSalary of(String spaceId, String month, String fromUser, String thanks) {
        CoupleBoardSalary row = new CoupleBoardSalary();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.month = month;
        row.fromUser = fromUser;
        row.thanks = thanks;
        row.created = System.currentTimeMillis();
        return row;
    }
}
