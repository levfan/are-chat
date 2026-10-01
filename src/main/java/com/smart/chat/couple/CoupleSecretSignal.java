package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 动作暗语本（F174）：约定一个动作=一句话。 */
@Data
@TableName("couple_secret_signal")
public class CoupleSecretSignal {

    public static final int SIGNAL_MAX = 50;
    public static final int MEANING_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String signal;
    private String meaning;
    private Long created;

    public static CoupleSecretSignal of(String spaceId, String fromUser, String signal, String meaning) {
        CoupleSecretSignal row = new CoupleSecretSignal();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.signal = signal;
        row.meaning = meaning;
        row.created = System.currentTimeMillis();
        return row;
    }
}
