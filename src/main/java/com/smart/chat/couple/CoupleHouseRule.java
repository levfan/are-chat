package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 家规宪法条款（F196）：条款/修正案，对方签字后生效为「约法」。 */
@Data
@TableName("couple_house_rule")
public class CoupleHouseRule {

    public static final String KIND_RULE = "RULE";
    public static final String KIND_AMENDMENT = "AMENDMENT";

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String kind;
    /** 修正案针对的原条款ID（可空） */
    private String refId;
    private String content;
    private String proposedBy;
    /** 0 未签 / 1 已签 */
    private Integer signed;
    /** 签字人（可空） */
    private String signedBy;
    private Long updatedAt;
    private Long created;

    public static CoupleHouseRule of(String spaceId, String kind, String refId, String content, String proposedBy) {
        CoupleHouseRule row = new CoupleHouseRule();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.kind = kind;
        row.refId = refId;
        row.content = content;
        row.proposedBy = proposedBy;
        row.signed = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
