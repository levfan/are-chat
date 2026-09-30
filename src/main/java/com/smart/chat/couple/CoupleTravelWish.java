package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 旅行心愿地图（F75）：把「想去」钉在地图上，一个个走成「去过」。 */
@Data
@TableName("couple_travel_wish")
public class CoupleTravelWish {

    public static final int PLACE_MAX = 100;
    public static final int TODO_MAX = 200;
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String place;
    private String wantTodo;
    private boolean visited;
    private Long visitedAt;
    private String visitedNote;
    private Long created;

    public static CoupleTravelWish of(String spaceId, String fromUser, String place, String wantTodo) {
        CoupleTravelWish row = new CoupleTravelWish();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.place = place;
        row.wantTodo = wantTodo;
        row.created = System.currentTimeMillis();
        return row;
    }
}
