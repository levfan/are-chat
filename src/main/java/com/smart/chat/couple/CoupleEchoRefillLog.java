package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F351 能量补给领取日志：每人每天一次（uk(space_id,from_user,day) 兜底，重复 400）。 */
@Data
@TableName("couple_echo_refill_log")
public class CoupleEchoRefillLog {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 领取日 yyyy-MM-dd */
    private String day;
    private Long created;

    public static CoupleEchoRefillLog of(String spaceId, String fromUser, String day) {
        CoupleEchoRefillLog row = new CoupleEchoRefillLog();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = day;
        row.created = System.currentTimeMillis();
        return row;
    }
}
