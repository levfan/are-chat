package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F356 夸夸回执：对 F54 夸夸墙（couple_praise，跨模块只读）某句点「收到」，uk 保证幂等。 */
@Data
@TableName("couple_echo_receipt")
public class CoupleEchoReceipt {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 夸夸墙条目 id（关联 couple_praise.id） */
    private String quoteId;
    private String fromUser;
    private Long created;

    public static CoupleEchoReceipt of(String spaceId, String quoteId, String fromUser) {
        CoupleEchoReceipt row = new CoupleEchoReceipt();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.quoteId = quoteId;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        return row;
    }
}
