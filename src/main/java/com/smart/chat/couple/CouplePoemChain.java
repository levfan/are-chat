package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情诗接龙（F160）：每天每人一句，连成我们的诗。 */
@Data
@TableName("couple_poem_chain")
public class CouplePoemChain {

    public static final int LINE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String line;
    private Long created;

    public static CouplePoemChain of(String spaceId, String day, String fromUser, String line) {
        CouplePoemChain row = new CouplePoemChain();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.line = line;
        row.created = System.currentTimeMillis();
        return row;
    }
}
