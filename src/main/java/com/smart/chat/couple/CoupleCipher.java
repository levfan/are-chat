package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 暗号小本本：只有彼此懂的暗号/梗/悄悄约定。 */
@Data
@TableName("couple_cipher")
public class CoupleCipher {

    public static final int KEYWORD_MAX = 40;
    public static final int MEANING_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String keyword;
    private String meaning;
    private String createdBy;
    private Long created;

    public static CoupleCipher of(String spaceId, String createdBy, String keyword, String meaning) {
        CoupleCipher row = new CoupleCipher();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.createdBy = createdBy;
        row.keyword = keyword;
        row.meaning = meaning;
        row.created = System.currentTimeMillis();
        return row;
    }
}
