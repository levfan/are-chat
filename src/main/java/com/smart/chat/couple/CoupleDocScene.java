package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 恋爱纪录片分镜（F190）：把一段回忆写成三幕剧本。 */
@Data
@TableName("couple_doc_scene")
public class CoupleDocScene {

    public static final int TITLE_MAX = 60;
    public static final int ACT_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private String actOne;
    private String actTwo;
    private String actThree;
    private String fromUser;
    private Long updatedAt;
    private Long created;

    public static CoupleDocScene of(String spaceId, String fromUser, String title, String actOne, String actTwo, String actThree) {
        CoupleDocScene row = new CoupleDocScene();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.actOne = actOne;
        row.actTwo = actTwo;
        row.actThree = actThree;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
