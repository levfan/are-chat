package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 承诺博物馆（F124）：郑重的承诺，双方都盖章后永久展出。 */
@Data
@TableName("couple_oath")
public class CoupleOath {

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private Integer stampA;
    private Integer stampB;
    private Long created;

    public static CoupleOath of(String spaceId, String fromUser, String content) {
        CoupleOath row = new CoupleOath();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.stampA = 0;
        row.stampB = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean fullyStamped() {
        return Integer.valueOf(1).equals(stampA) && Integer.valueOf(1).equals(stampB);
    }
}
