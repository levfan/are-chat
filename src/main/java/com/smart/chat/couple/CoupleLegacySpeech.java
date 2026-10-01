package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F342 续约发布会：年度发言稿 + 对方按评分卡打分。 */
@Data
@TableName("couple_legacy_speech")
public class CoupleLegacySpeech {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String text;
    private Integer score;
    private String scoreNote;
    private String ratedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacySpeech of(String spaceId, String year, String fromUser, String text) {
        CoupleLegacySpeech row = new CoupleLegacySpeech();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.text = text;
        row.score = null;
        row.scoreNote = "";
        row.ratedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isRated() {
        return ratedBy != null && !ratedBy.isEmpty();
    }
}
