package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 情话Battle 参赛句子（F136）。 */
@Data
@TableName("couple_sweet_line")
public class CoupleSweetLine {

    public static final int CONTENT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String battleId;
    private String fromUser;
    private String content;
    private Long created;

    public static CoupleSweetLine of(String spaceId, String battleId, String fromUser, String content) {
        CoupleSweetLine row = new CoupleSweetLine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.battleId = battleId;
        row.fromUser = fromUser;
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
