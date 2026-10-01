package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F213 踩雷库：一起拉黑的小店，谁提议谁有权划掉。 */
@Data
@TableName("couple_dine_nogo")
public class CoupleDineNogo {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String reason;
    private String fromUser;
    private Long created;

    public static CoupleDineNogo of(String spaceId, String name, String reason, String fromUser) {
        CoupleDineNogo row = new CoupleDineNogo();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.reason = reason;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();

        return row;
    }
}
