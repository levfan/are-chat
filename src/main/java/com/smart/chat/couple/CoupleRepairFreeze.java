package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F320 冷冻解冻规程：吵架挂 3-24 小时冷冻，解冻要双人签 + 三问走完。 */
@Data
@TableName("couple_repair_freeze")
public class CoupleRepairFreeze {

    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_THAWED = "THAWED";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String startDay;
    private String fromUser;
    private Integer hours;
    private Long untilAt;
    private String reason;
    private String answer1;
    private String answer2;
    private String answer3;
    private Integer signedA;
    private Integer signedB;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleRepairFreeze of(String spaceId, String startDay, String fromUser,
                                        int hours, long untilAt, String reason) {
        CoupleRepairFreeze row = new CoupleRepairFreeze();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.startDay = startDay;
        row.fromUser = fromUser;
        row.hours = hours;
        row.untilAt = untilAt;
        row.reason = reason == null ? "" : reason;
        row.answer1 = "";
        row.answer2 = "";
        row.answer3 = "";
        row.signedA = 0;
        row.signedB = 0;
        row.status = STATUS_FROZEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isSignedA() {
        return signedA != null && signedA == 1;
    }

    public boolean isSignedB() {
        return signedB != null && signedB == 1;
    }

    /** 双人签齐才放行解冻。 */
    public boolean isBothSigned() {
        return isSignedA() && isSignedB();
    }
}
