package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F268 休战旗：举旗冻结（默认 30 分钟），到点双方各选继续/算了；在途仅一面旗。 */
@Data
@TableName("couple_truce")
public class CoupleTruce {

    public static final String STATUS_ON = "ON";
    public static final String STATUS_ENDED = "ENDED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String raiser;
    private Long untilAt;
    private Integer decideA;
    private Integer decideB;
    private String status;
    private Long endedAt;
    private Long created;

    public static CoupleTruce of(String spaceId, String raiser, long untilAt) {
        CoupleTruce row = new CoupleTruce();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.raiser = raiser;
        row.untilAt = untilAt;
        row.status = STATUS_ON;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean isExpired(long now) {
        return untilAt != null && untilAt <= now;
    }
}
