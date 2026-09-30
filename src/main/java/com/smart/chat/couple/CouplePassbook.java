package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** 恋爱存折（F71）：每天各存一笔「今天为这段感情做的一件小事」。 */
@Data
@TableName("couple_passbook")
public class CouplePassbook {

    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String day;
    private String content;
    private Long created;

    public static CouplePassbook of(String spaceId, String fromUser, String content) {
        CouplePassbook row = new CouplePassbook();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = LocalDate.now().toString();
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
