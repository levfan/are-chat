package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F275 服药提醒链：TA 提醒了 + 本人吃了 = 链 +1，断日清零。 */
@Data
@TableName("couple_medicine")
public class CoupleMedicine {

    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_STOPPED = "STOPPED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String times;
    private String fromUser;
    private String status;
    private String lastRemindDay;
    private String lastTakenDay;
    private Integer streak;
    private Long created;

    public static CoupleMedicine of(String spaceId, String name, String times, String fromUser) {
        CoupleMedicine row = new CoupleMedicine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.times = times == null ? "" : times;
        row.fromUser = fromUser;
        row.status = STATUS_ONGOING;
        row.lastRemindDay = "";
        row.lastTakenDay = "";
        row.streak = 0;
        row.created = System.currentTimeMillis();
        return row;
    }
}
