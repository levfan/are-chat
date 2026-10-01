package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F345 情侣品牌：关系命名 + slogan + 产品简介，双确认后发布到空间头部。 */
@Data
@TableName("couple_legacy_brand")
public class CoupleLegacyBrand {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String slogan;
    private String intro;
    private Integer published;
    private String byUser;
    private String confirmedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyBrand of(String spaceId, String name, String slogan, String intro, String byUser) {
        CoupleLegacyBrand row = new CoupleLegacyBrand();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.slogan = slogan;
        row.intro = intro;
        row.published = 0;
        row.byUser = byUser;
        row.confirmedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isPublished() {
        return published != null && published == 1;
    }
}
