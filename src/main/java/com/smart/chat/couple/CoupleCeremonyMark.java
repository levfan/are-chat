package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F233 庆祝打卡：过法卡当日逐条打勾。 */
@Data
@TableName("couple_ceremony_mark")
public class CoupleCeremonyMark {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String ritualId;
    private String day;
    private Long created;

    public static CoupleCeremonyMark of(String spaceId, String ritualId, String day) {
        CoupleCeremonyMark row = new CoupleCeremonyMark();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.ritualId = ritualId;
        row.day = day;
        row.created = System.currentTimeMillis();
        return row;
    }
}
