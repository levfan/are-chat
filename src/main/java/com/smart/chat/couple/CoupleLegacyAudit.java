package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F341 记忆库年审：最想删/最想留各限 3 条。 */
@Data
@TableName("couple_legacy_audit")
public class CoupleLegacyAudit {

    public static final int THREE_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String keepThree;
    private String deleteThree;
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyAudit of(String spaceId, String year, String fromUser) {
        CoupleLegacyAudit row = new CoupleLegacyAudit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.keepThree = "";
        row.deleteThree = "";
        row.note = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
