package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 通知免打扰时段（F197）：每人一份，静音时段内前端不弹窗，消息照常留存。 */
@Data
@TableName("couple_dnd_setting")
public class CoupleDndSetting {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** HH:mm */
    private String startTime;
    /** HH:mm（可跨零点） */
    private String endTime;
    /** 0 停用 / 1 启用 */
    private Integer enabled;
    private Long updatedAt;
    private Long created;

    public static CoupleDndSetting of(String spaceId, String fromUser, String startTime, String endTime, int enabled) {
        CoupleDndSetting row = new CoupleDndSetting();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.startTime = startTime;
        row.endTime = endTime;
        row.enabled = enabled;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 给定时刻是否落在免打扰窗口内（支持跨零点）。 */
    public boolean covers(String hhmm) {
        if (!Integer.valueOf(1).equals(enabled)) {
            return false;
        }
        String s = startTime;
        String e = endTime;
        return s.compareTo(e) <= 0
                ? hhmm.compareTo(s) >= 0 && hhmm.compareTo(e) < 0
                : hhmm.compareTo(s) >= 0 || hhmm.compareTo(e) < 0;
    }
}
