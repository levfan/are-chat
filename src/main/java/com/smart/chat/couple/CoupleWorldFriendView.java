package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F332 朋友视角问卷：三题「外人怎么看我们」，线下问友回填出他观卡。 */
@Data
@TableName("couple_world_friend_view")
public class CoupleWorldFriendView {

    public static final int SLOT_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private Integer slot;
    private String question;
    private String askedTo;
    private String answer;
    private String byUser;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldFriendView of(String spaceId, int slot, String question) {
        CoupleWorldFriendView row = new CoupleWorldFriendView();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.slot = slot;
        row.question = question == null ? "" : question;
        row.askedTo = "";
        row.answer = "";
        row.byUser = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isFilled() {
        return answer != null && !answer.isEmpty();
    }
}
