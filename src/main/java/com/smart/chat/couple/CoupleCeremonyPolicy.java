package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F234 爱情保险柜：每月互夸各 1 句=交齐当月保费。 */
@Data
@TableName("couple_ceremony_policy")
public class CoupleCeremonyPolicy {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String month;
    private String fromUser;
    private String quote;
    private Long created;

    public static CoupleCeremonyPolicy of(String spaceId, String month, String fromUser, String quote) {
        CoupleCeremonyPolicy row = new CoupleCeremonyPolicy();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.month = month;
        row.fromUser = fromUser;
        row.quote = quote;
        row.created = System.currentTimeMillis();
        return row;
    }
}
