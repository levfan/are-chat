package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F235 续约仪式：每满 100 天/周年双方各签一句「我还是选你」。 */
@Data
@TableName("couple_ceremony_renew")
public class CoupleCeremonyRenew {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String anchorDay;
    private String fromUser;
    private String line;
    private Long created;

    public static CoupleCeremonyRenew of(String spaceId, String anchorDay, String fromUser, String line) {
        CoupleCeremonyRenew row = new CoupleCeremonyRenew();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.anchorDay = anchorDay;
        row.fromUser = fromUser;
        row.line = line;
        row.created = System.currentTimeMillis();
        return row;
    }
}
