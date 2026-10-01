package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F293 想象中的家：字段化梦想家，一年一版。 */
@Data
@TableName("couple_dream_home")
public class CoupleDreamHome {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String rooms;
    private String windowView;
    private String smell;
    private String corner;
    private Long created;
    private Long updatedAt;

    public static CoupleDreamHome of(String spaceId, String year, String fromUser) {
        CoupleDreamHome row = new CoupleDreamHome();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.rooms = "";
        row.windowView = "";
        row.smell = "";
        row.corner = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
