package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 数字密码情书（F164）：把情话藏进数字密码，对方解码。 */
@Data
@TableName("couple_cipher_note")
public class CoupleCipherNote {

    public static final int CIPHER_MAX = 500;
    public static final int HINT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String cipher;
    private String hint;
    private String decodedBy;
    private Long decodedAt;
    private Long created;

    public static CoupleCipherNote of(String spaceId, String fromUser, String cipher, String hint) {
        CoupleCipherNote row = new CoupleCipherNote();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.cipher = cipher;
        row.hint = hint;
        row.created = System.currentTimeMillis();
        return row;
    }
}
