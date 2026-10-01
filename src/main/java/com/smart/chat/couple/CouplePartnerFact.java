package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** TA 使用手册（F143）：口味/雷区/心头好/小怪癖档案，双方互补。 */
@Data
@TableName("couple_partner_fact")
public class CouplePartnerFact {

    public static final String KIND_TASTE = "TASTE";
    public static final String KIND_NOGO = "NOGO";
    public static final String KIND_FAV = "FAV";
    public static final String KIND_QUIRK = "QUIRK";
    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String kind;
    private String content;
    private Long created;

    public static CouplePartnerFact of(String spaceId, String fromUser, String kind, String content) {
        CouplePartnerFact row = new CouplePartnerFact();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.kind = kind;
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
