package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 故事接龙（F104）：轮流写一句，chain_id = 首句 id，完结后可开新篇。 */
@Data
@TableName("couple_story_line")
public class CoupleStoryLine {

    public static final int CONTENT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String chainId;
    private Integer seq;
    private String byUser;
    private String content;
    private Integer isFinal;
    private Long created;

    public static CoupleStoryLine of(String spaceId, String chainId, int seq,
                                     String byUser, String content, boolean isFinal) {
        CoupleStoryLine row = new CoupleStoryLine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.chainId = chainId;
        row.seq = seq;
        row.byUser = byUser;
        row.content = content;
        row.isFinal = isFinal ? 1 : 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isFinalLine() {
        return Integer.valueOf(1).equals(isFinal);
    }
}
