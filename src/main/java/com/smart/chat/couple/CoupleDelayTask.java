package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 拖延互助所（F155）：登记拖延的事，对方催办（1h 冷却），完成庆祝。 */
@Data
@TableName("couple_delay_task")
public class CoupleDelayTask {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    public static final int TITLE_MAX = 100;
    /** 催办冷却毫秒：1 小时 */
    public static final long NAG_COOLDOWN_MS = 60 * 60 * 1000L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String title;
    private String deadlineDay;
    private Integer nagCount;
    private Long lastNagAt;
    private String status;
    private Long doneAt;
    private Long created;

    public static CoupleDelayTask of(String spaceId, String fromUser, String title, String deadlineDay) {
        CoupleDelayTask row = new CoupleDelayTask();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.deadlineDay = deadlineDay;
        row.nagCount = 0;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
