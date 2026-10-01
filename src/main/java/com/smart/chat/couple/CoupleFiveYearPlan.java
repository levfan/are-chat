package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 五年计划双轨（F187）：自己的 MINE + 我们的 OURS，OURS 各自认领。 */
@Data
@TableName("couple_five_year_plan")
public class CoupleFiveYearPlan {

    public static final String TRACK_MINE = "MINE";
    public static final String TRACK_OURS = "OURS";

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String track;
    private String fromUser;
    private String content;
    /** OURS 轨道认领人（可空） */
    private String ownerUser;
    /** 0 进行中 / 1 已达成 */
    private Integer done;
    private Long updatedAt;
    private Long created;

    public static CoupleFiveYearPlan of(String spaceId, String track, String fromUser, String content) {
        CoupleFiveYearPlan row = new CoupleFiveYearPlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.track = track;
        row.fromUser = fromUser;
        row.content = content;
        row.done = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
