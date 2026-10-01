package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F239 当日体感：仪式当天各留一句「此刻感觉」，次年今日对比。 */
@Data
@TableName("couple_ceremony_recap")
public class CoupleCeremonyRecap {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String feeling;
    private Long created;

    public static CoupleCeremonyRecap of(String spaceId, String day, String fromUser, String feeling) {
        CoupleCeremonyRecap row = new CoupleCeremonyRecap();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.feeling = feeling;
        row.created = System.currentTimeMillis();
        return row;
    }
}
