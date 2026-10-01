package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F232 过法任务卡：每个小日子写死 1-3 条庆祝方式。 */
@Data
@TableName("couple_ceremony_ritual")
public class CoupleCeremonyRitual {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String foundedId;
    private String content;
    private Long created;

    public static CoupleCeremonyRitual of(String spaceId, String foundedId, String content) {
        CoupleCeremonyRitual row = new CoupleCeremonyRitual();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.foundedId = foundedId;
        row.content = content;
        row.created = System.currentTimeMillis();
        return row;
    }
}
