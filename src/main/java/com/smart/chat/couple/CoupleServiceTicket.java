package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F308 今日客服：下单 → 30 分钟内响应 → 顾客评分 → 差评可申诉。 */
@Data
@TableName("couple_service_ticket")
public class CoupleServiceTicket {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ANSWERED = "ANSWERED";
    public static final String STATUS_RATED = "RATED";
    public static final String STATUS_APPEALED = "APPEALED";

    /** 准时响应窗口：30 分钟。 */
    public static final long ON_TIME_MS = 30 * 60 * 1000L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String note;
    private String customerUser;
    private String status;
    private Long answeredAt;
    private Integer onTime;
    private Integer score;
    private String appeal;
    private Long created;
    private Long updatedAt;

    public static CoupleServiceTicket of(String spaceId, String note, String customerUser) {
        CoupleServiceTicket row = new CoupleServiceTicket();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.note = note;
        row.customerUser = customerUser;
        row.status = STATUS_OPEN;
        row.onTime = 0;
        row.appeal = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
