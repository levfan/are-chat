package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F354 感谢慢递：想谢的话封存 7 天后送达，在途每人 ≤3 封，到日由 GET 读时惰性结算。 */
@Data
@TableName("couple_echo_slow")
public class CoupleEchoSlow {

    public static final int CONTENT_MAX = 100;
    public static final int IN_FLIGHT_MAX = 3;
    public static final int DELIVER_AFTER_DAYS = 7;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 收信的人=对方 */
    private String toUser;
    private String content;
    /** 送达日=写信日+7 yyyy-MM-dd */
    private String openDay;
    /** 1=已送达（读时惰性结算） */
    private Integer delivered;
    private Long created;
    private Long updatedAt;

    public static CoupleEchoSlow of(String spaceId, String fromUser, String toUser, String content, String openDay) {
        CoupleEchoSlow row = new CoupleEchoSlow();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.toUser = toUser;
        row.content = content;
        row.openDay = openDay;
        row.delivered = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isDelivered() {
        return delivered != null && delivered == 1;
    }
}
