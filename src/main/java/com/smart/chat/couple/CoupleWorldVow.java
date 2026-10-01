package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F337 社会信用：公开声明「我保证不做…」+ TA 见证 + 到期解除或塌房。 */
@Data
@TableName("couple_world_vow")
public class CoupleWorldVow {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_KEPT = "KEPT";
    public static final String STATUS_BROKEN = "BROKEN";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String content;
    private String ownerUser;
    private String dueDay;
    private Integer witnessed;
    private String status;
    private String brokenNote;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldVow of(String spaceId, String content, String ownerUser, String dueDay) {
        CoupleWorldVow row = new CoupleWorldVow();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.content = content;
        row.ownerUser = ownerUser;
        row.dueDay = dueDay;
        row.witnessed = 0;
        row.status = STATUS_OPEN;
        row.brokenNote = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isWitnessed() {
        return witnessed != null && witnessed == 1;
    }
}
