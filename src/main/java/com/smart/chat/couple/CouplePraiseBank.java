package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 优点存折（F158）：平时存对方优点，吵架时读三条。 */
@Data
@TableName("couple_praise_bank")
public class CouplePraiseBank {

    public static final int CONTENT_MAX = 200;
    public static final int SCENE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private String scene;
    private Long created;

    public static CouplePraiseBank of(String spaceId, String fromUser, String content, String scene) {
        CouplePraiseBank row = new CouplePraiseBank();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.scene = scene;
        row.created = System.currentTimeMillis();
        return row;
    }
}
