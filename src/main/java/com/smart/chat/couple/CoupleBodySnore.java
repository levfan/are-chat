package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** F311 呼噜自报：晨起各报档位，对方补「震感」点评。 */
@Data
@TableName("couple_body_snore")
public class CoupleBodySnore {

    public static final List<String> LEVELS = List.of("NONE", "TINY", "MID", "HEAVY");

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String levelA;
    private String levelB;
    private String shakeA;
    private String shakeB;
    private Long created;
    private Long updatedAt;

    public static CoupleBodySnore of(String spaceId, String day) {
        CoupleBodySnore row = new CoupleBodySnore();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.levelA = "";
        row.levelB = "";
        row.shakeA = "";
        row.shakeB = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 震感级数（档位换算，NONE=0 没报=-1）。 */
    public int shakeScore(boolean sideA) {
        return LEVELS.indexOf(sideA ? levelA : levelB);
    }
}
