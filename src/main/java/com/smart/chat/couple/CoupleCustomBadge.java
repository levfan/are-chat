package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 自定义成就（F148）：自设成就+达成条件，达成颁发双人证书。 */
@Data
@TableName("couple_custom_badge")
public class CoupleCustomBadge {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ISSUED = "ISSUED";
    public static final int TITLE_MAX = 50;
    public static final int CONDITION_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String title;
    private String condition;
    private String status;
    private Long issuedAt;
    private Long created;

    public static CoupleCustomBadge of(String spaceId, String fromUser, String title, String condition) {
        CoupleCustomBadge row = new CoupleCustomBadge();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.condition = condition;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
