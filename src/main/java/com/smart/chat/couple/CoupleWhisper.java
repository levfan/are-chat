package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 匿名树洞（F67）：匿名（或实名）向 TA 提一个不敢问的问题，TA 回答后揭晓提问人。 */
@Data
@TableName("couple_whisper")
public class CoupleWhisper {

    public static final int QUESTION_MAX = 200;
    public static final int ANSWER_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String question;
    private boolean anonymous;
    private String answer;
    private Long answeredAt;
    private Long created;

    public static CoupleWhisper of(String spaceId, String fromUser, String question, boolean anonymous) {
        CoupleWhisper row = new CoupleWhisper();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.question = question;
        row.anonymous = anonymous;
        row.created = System.currentTimeMillis();
        return row;
    }
}
