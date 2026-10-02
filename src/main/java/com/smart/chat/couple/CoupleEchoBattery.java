package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F357 电量预报：每人每天一格，level 1-5 钳制；对方 ≤2 格时读时给「今晚轻轻的」提示行。 */
@Data
@TableName("couple_echo_battery")
public class CoupleEchoBattery {

    public static final int LEVEL_MIN = 1;
    public static final int LEVEL_MAX = 5;
    public static final int LEVEL_DEFAULT = 3;
    public static final int LOW_LEVEL = 2;
    public static final int WANT_MAX = 40;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 预报日 yyyy-MM-dd */
    private String day;
    private String fromUser;
    private Integer level;
    /** 今天想被怎样对待 */
    private String want;
    private Long created;
    private Long updatedAt;

    public static CoupleEchoBattery of(String spaceId, String day, String fromUser, int level, String want) {
        CoupleEchoBattery row = new CoupleEchoBattery();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.level = level;
        row.want = want;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
