package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F339 代 TA 赔礼：与 TA 亲友有误会，代写赔礼信经 TA 审阅才算送达。 */
@Data
@TableName("couple_world_apology")
public class CoupleWorldApology {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_BACK = "BACK";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String toPerson;
    private String reason;
    private String draft;
    private String fromUser;
    private String reviewNote;
    private String reviewedBy;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldApology of(String spaceId, String toPerson, String reason, String draft, String fromUser) {
        CoupleWorldApology row = new CoupleWorldApology();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.toPerson = toPerson;
        row.reason = reason == null ? "" : reason;
        row.draft = draft;
        row.fromUser = fromUser;
        row.reviewNote = "";
        row.reviewedBy = "";
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
