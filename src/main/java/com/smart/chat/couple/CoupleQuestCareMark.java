package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F373 陪护代记：生病期间的喝水/吃药由陪护人代记，一天每种只记一次。
 * 注意：本表只有 created 没有 updated_at（打卡一次即定，实体里也不得有 updatedAt 字段）。
 */
@Data
@TableName("couple_quest_care_mark")
public class CoupleQuestCareMark {

    public static final String KIND_WATER = "WATER";
    public static final String KIND_MED = "MED";
    public static final List<String> KINDS = List.of(KIND_WATER, KIND_MED);

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 所属陪护单（关联 couple_quest_nurse.id） */
    private String nurseId;
    /** 打卡日 yyyy-MM-dd */
    private String day;
    /** WATER 喝水 / MED 吃药（服务层校验） */
    private String kind;
    /** 代记的人=陪护人 */
    private String byUser;
    private Long created;

    public static CoupleQuestCareMark of(String spaceId, String nurseId, String day, String kind, String byUser) {
        CoupleQuestCareMark row = new CoupleQuestCareMark();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.nurseId = nurseId;
        row.day = day;
        row.kind = kind;
        row.byUser = byUser;
        row.created = System.currentTimeMillis();
        return row;
    }
}
