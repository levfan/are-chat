package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F291 人生大事册。 */
@Data
@TableName("couple_bucket")
public class CoupleBucket {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_GONE = "GONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String targetDay;
    private String note;
    private String ownerUser;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleBucket of(String spaceId, String name, String targetDay, String note, String ownerUser) {
        CoupleBucket row = new CoupleBucket();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.targetDay = targetDay == null ? "" : targetDay;
        row.note = note == null ? "" : note;
        row.ownerUser = ownerUser;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
