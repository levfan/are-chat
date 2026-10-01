package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** F318 情绪药友：周记式「最近药怎么样」互助（陪伴话术，非医嘱）。 */
@Data
@TableName("couple_body_med")
public class CoupleBodyMed {

    public static final List<String> HOWS = List.of("STEADY", "HARD", "NONE");

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String fromUser;
    private String how;
    private String note;
    private String reply;
    private String replyBy;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyMed of(String spaceId, String week, String fromUser) {
        CoupleBodyMed row = new CoupleBodyMed();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.fromUser = fromUser;
        row.how = "";
        row.note = "";
        row.reply = "";
        row.replyBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
