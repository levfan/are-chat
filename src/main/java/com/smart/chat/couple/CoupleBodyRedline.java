package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F316 忌口红线本：过敏/忌口一条一行，饭桌饭票池读同表拼标。 */
@Data
@TableName("couple_body_redline")
public class CoupleBodyRedline {

    public static final String KIND_ALLERGY = "ALLERGY";
    public static final String KIND_AVOID = "AVOID";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String item;
    private String kind;
    private String note;
    private String fromUser;
    private Long created;

    public static CoupleBodyRedline of(String spaceId, String item, String kind, String note, String fromUser) {
        CoupleBodyRedline row = new CoupleBodyRedline();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.item = item;
        row.kind = (kind == null || kind.isBlank()) ? KIND_AVOID : kind;
        row.note = note == null ? "" : note;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        return row;
    }
}
