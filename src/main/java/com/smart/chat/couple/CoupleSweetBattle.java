package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情话Battle（F136）：今日擂台，双方各发一句，互相投票定胜负。 */
@Data
@TableName("couple_sweet_battle")
public class CoupleSweetBattle {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_FULL = "FULL";
    public static final String STATUS_DONE = "DONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String status;
    private String voteA;
    private String voteB;
    private String winner;
    private Long created;

    public static CoupleSweetBattle of(String spaceId, String day) {
        CoupleSweetBattle row = new CoupleSweetBattle();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
