package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F324 和好了倒计时：冷战开 10-60 分钟倒计时，对方可暂停/提前，到点递台阶卡。 */
@Data
@TableName("couple_repair_makeup")
public class CoupleRepairMakeup {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_ENDED = "ENDED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer minutes;
    private Long startAt;
    private Integer paused;
    private String pausedBy;
    private String stepCard;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleRepairMakeup of(String spaceId, String day, String fromUser, int minutes, long startAt) {
        CoupleRepairMakeup row = new CoupleRepairMakeup();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.minutes = minutes;
        row.startAt = startAt;
        row.paused = 0;
        row.pausedBy = "";
        row.stepCard = "";
        row.status = STATUS_RUNNING;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean pausedFlag() {
        return paused != null && paused == 1;
    }

    /** 到点时刻（起算 + 时长）。 */
    public long endAt() {
        return startAt + minutes * 60_000L;
    }
}
