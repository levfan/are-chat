package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 爱情刮刮乐（F50）：每周系统给双方各发一张「来自 TA」的奖励券，
 * 收券人刮开才知道是什么，把券递给对方核销——把日常变成可兑现的小约定。
 */
@Data
@TableName("couple_scratch")
public class CoupleScratch {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 周标识（如 2026-W40），每周一张 */
    private String weekKey;
    /** 送券人（对方） */
    private String fromUser;
    /** 收券人（刮券的人） */
    private String owner;
    private String prizeKind;
    private String prizeText;
    private boolean scratched;
    private Long scratchedAt;
    /** 核销时间（空 = 还没用） */
    private Long redeemedAt;
    private Long created;

    public static CoupleScratch of(String spaceId, String weekKey, String fromUser, String owner,
                                   String prizeKind, String prizeText) {
        CoupleScratch row = new CoupleScratch();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.weekKey = weekKey;
        row.fromUser = fromUser;
        row.owner = owner;
        row.prizeKind = prizeKind;
        row.prizeText = prizeText;
        row.scratched = false;
        row.created = System.currentTimeMillis();
        return row;
    }
}
