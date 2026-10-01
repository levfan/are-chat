package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 三行情书（F161）：三行小诗，对方可点赞。 */
@Data
@TableName("couple_poem_3line")
public class CouplePoem3Line {

    public static final int LINE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String line1;
    private String line2;
    private String line3;
    private String likedBy;
    private Long likedAt;
    private Long created;

    public static CouplePoem3Line of(String spaceId, String fromUser, String l1, String l2, String l3) {
        CouplePoem3Line row = new CouplePoem3Line();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.line1 = l1;
        row.line2 = l2;
        row.line3 = l3;
        row.created = System.currentTimeMillis();
        return row;
    }
}
