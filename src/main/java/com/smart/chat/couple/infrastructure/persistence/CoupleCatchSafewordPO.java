package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F382 安全词约定：双方各约一个「暂停」词（uk(space,from_user) 每人一行，可改写）。
 * 使用流水在 couple_catch_safeword_use，本表只存约定本身。
 */
@Data
@TableName("couple_catch_safeword")
public class CoupleCatchSafewordPO {

    /** 安全词字数上限（列 varchar(80) 已按 4 倍宽度放宽） */
    public static final int WORD_MAX = 20;
    /** 「用了之后希望怎样」字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int NOTE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 约定这个词的人（区分大小写，每人一个） */
    private String fromUser;
    /** 安全词 */
    private String word;
    /** 用了之后希望怎样；空串=没说 */
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchSafewordPO of(String spaceId, String fromUser, String word, String note) {
        CoupleCatchSafewordPO row = new CoupleCatchSafewordPO();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.word = word;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
