package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 抽象画（F138）：前端按种子生成抽象画，双方互赠入馆。 */
@Data
@TableName("couple_art_gallery")
public class CoupleArtGallery {

    public static final int TITLE_MAX = 50;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String title;
    private Integer seed;
    private Long created;

    public static CoupleArtGallery of(String spaceId, String fromUser, String title, int seed) {
        CoupleArtGallery row = new CoupleArtGallery();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.seed = seed;
        row.created = System.currentTimeMillis();
        return row;
    }
}
