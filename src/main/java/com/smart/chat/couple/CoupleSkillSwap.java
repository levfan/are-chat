package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 技能交换所（F182）：我教你做饭你教我修电脑。 */
@Data
@TableName("couple_skill_swap")
public class CoupleSkillSwap {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_TAKEN = "TAKEN";
    public static final String STATUS_DONE = "DONE";

    public static final int SKILL_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 我能教你什么 */
    private String teach;
    /** 我想跟你学什么 */
    private String learn;
    private String status;
    private Long updatedAt;
    private Long created;

    public static CoupleSkillSwap of(String spaceId, String fromUser, String teach, String learn) {
        CoupleSkillSwap row = new CoupleSkillSwap();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.teach = teach;
        row.learn = learn;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
