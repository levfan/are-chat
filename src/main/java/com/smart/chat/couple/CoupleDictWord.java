package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 恋爱词典（F78）：我们才懂的语言，值得一本词典。 */
@Data
@TableName("couple_dict_word")
public class CoupleDictWord {

    public static final int WORD_MAX = 50;
    public static final int MEANING_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String word;
    private String meaning;
    private Long created;

    public static CoupleDictWord of(String spaceId, String fromUser, String word, String meaning) {
        CoupleDictWord row = new CoupleDictWord();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.word = word;
        row.meaning = meaning;
        row.created = System.currentTimeMillis();
        return row;
    }
}
