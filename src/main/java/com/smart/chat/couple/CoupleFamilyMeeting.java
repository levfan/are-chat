package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 家庭会议纪要（F180）：每周议题+决议+跟进日，可关闭。 */
@Data
@TableName("couple_family_meeting")
public class CoupleFamilyMeeting {

    public static final int TOPIC_MAX = 100;
    public static final int DECISION_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 所属周（周一日期 yyyy-MM-dd） */
    private String week;
    private String topic;
    private String decision;
    /** 跟进日（可空） */
    private String followDay;
    private String raisedBy;
    /** 0 进行中 / 1 已关闭 */
    private Integer closed;
    private Long created;
    private Long updatedAt;

    public static CoupleFamilyMeeting of(String spaceId, String week, String raisedBy, String topic) {
        CoupleFamilyMeeting row = new CoupleFamilyMeeting();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.raisedBy = raisedBy;
        row.topic = topic;
        row.decision = "";
        row.closed = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
