package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F260 想被听时段：申请-确认-聊完-互评，在途同时仅 1 个。 */
@Data
@TableName("couple_listen_slot")
public class CoupleListenSlot {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String topic;
    private String status;
    private Long confirmedAt;
    private Long doneAt;
    private Integer rateMine;
    private Integer ratePartner;
    private String note;
    private Long created;

    public static CoupleListenSlot of(String spaceId, String day, String fromUser, String topic) {
        CoupleListenSlot row = new CoupleListenSlot();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.topic = topic == null ? "" : topic;
        row.status = STATUS_OPEN;
        row.note = "";
        row.created = System.currentTimeMillis();
        return row;
    }
}
