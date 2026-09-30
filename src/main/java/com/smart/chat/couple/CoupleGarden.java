package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 爱情花园（F54）：两个人共同浇灌的一棵小树。
 * 双方每人每天可浇一次水，浇满升级；连续 3 天没人浇水会蔫，再浇即复活。
 * A/B 浇水日的归属由 Service 依据 couple_space.userA/userB 写入。
 */
@Data
@TableName("couple_garden")
public class CoupleGarden {

    /** 最高阶段 */
    public static final int STAGE_MAX = 6;
    /** 每升一阶段需要的浇水次数 */
    public static final int WATER_PER_STAGE = 7;
    /** 连续这么多天没人浇水就蔫掉 */
    public static final int WITHER_AFTER_DAYS = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 成长阶段 0-6 */
    private int stage;
    private int totalWater;
    private String lastWaterDayA;
    private String lastWaterDayB;
    private boolean withered;
    private int revivedCount;
    private Long created;
    private Long updatedAt;

    public static CoupleGarden of(String spaceId) {
        CoupleGarden row = new CoupleGarden();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.stage = 0;
        row.totalWater = 0;
        row.withered = false;
        row.created = System.currentTimeMillis();
        return row;
    }
}
