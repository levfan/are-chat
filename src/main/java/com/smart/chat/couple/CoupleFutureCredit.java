package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F299 未来信用卡：承诺未来小事，兑现提额逾期降额。 */
@Data
@TableName("couple_future_credit")
public class CoupleFutureCredit {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String promise;
    private String dueDay;
    private String fromUser;
    private String status;
    private Long keptAt;
    private Long created;
    private Long updatedAt;

    public static CoupleFutureCredit of(String spaceId, String promise, String dueDay, String fromUser) {
        CoupleFutureCredit row = new CoupleFutureCredit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.promise = promise;
        row.dueDay = dueDay;
        row.fromUser = fromUser;
        row.status = "OPEN";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
