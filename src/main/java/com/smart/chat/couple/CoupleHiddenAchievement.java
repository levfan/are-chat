package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 隐藏彩蛋成就解锁记录（F195）：达标即自动落库，一人解锁全空间可见。 */
@Data
@TableName("couple_hidden_achievement")
public class CoupleHiddenAchievement {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String code;
    private String unlockedBy;
    private Long created;

    public static CoupleHiddenAchievement of(String spaceId, String code, String unlockedBy) {
        CoupleHiddenAchievement row = new CoupleHiddenAchievement();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.code = code;
        row.unlockedBy = unlockedBy;
        row.created = System.currentTimeMillis();
        return row;
    }
}
