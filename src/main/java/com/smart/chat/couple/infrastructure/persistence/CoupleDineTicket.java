package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F210 今晚饭票：每人每天提名一道菜，两票撞同一菜=命中。 */
@Data
@TableName("couple_dine_ticket")
public class CoupleDineTicket {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String dish;
    private String reason;
    private Long created;

    public static CoupleDineTicket of(String spaceId, String day, String fromUser, String dish, String reason) {
        CoupleDineTicket row = new CoupleDineTicket();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.dish = dish;
        row.reason = reason;
        row.created = System.currentTimeMillis();

        return row;
    }
}
