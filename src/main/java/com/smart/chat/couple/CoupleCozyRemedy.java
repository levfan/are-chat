package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F227 疼痛对策本：各自登记我难受时的正确做法，对方不适日一键送达。 */
@Data
@TableName("couple_cozy_remedy")
public class CoupleCozyRemedy {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String forUser;
    private String body;
    private Long created;
    private Long updatedAt;

    public static CoupleCozyRemedy of(String spaceId, String forUser, String body) {
        CoupleCozyRemedy row = new CoupleCozyRemedy();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.forUser = forUser;
        row.body = body;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
