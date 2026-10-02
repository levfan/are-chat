package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F352 鼓励语罐：每人 5 格，第 6 条装不下；删除后腾出的槽位可复用。 */
@Data
@TableName("couple_echo_juice")
public class CoupleEchoJuice {

    public static final int CONTENT_MAX = 60;
    public static final int CAP = 5;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 罐子槽位 1-5（uk 占位） */
    private Integer idx;
    private String content;
    private Long created;
    private Long updatedAt;

    public static CoupleEchoJuice of(String spaceId, String fromUser, int idx, String content) {
        CoupleEchoJuice row = new CoupleEchoJuice();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.idx = idx;
        row.content = content;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
