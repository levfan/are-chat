package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F371 出关战报：一关一报（uk battle_id），结果 WIN/LOSE/SURVIVE，对方盖章才算翻篇。
 * 盖章口径：sealed_by 非空即已盖（NULL=还没盖），盖章同时写入 sealed_at 毫秒。
 */
@Data
@TableName("couple_quest_report")
public class CoupleQuestReport {

    public static final String RESULT_WIN = "WIN";
    public static final String RESULT_LOSE = "LOSE";
    public static final String RESULT_SURVIVE = "SURVIVE";
    public static final List<String> RESULTS = List.of(RESULT_WIN, RESULT_LOSE, RESULT_SURVIVE);
    /** 一句感受字数上限（列 varchar(180) 已按 3 倍宽度放宽） */
    public static final int FEELING_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 所属关卡（关联 couple_quest_battle.id，一战一报） */
    private String battleId;
    /** WIN 通关 / LOSE 没扛住 / SURVIVE 活着回来了（服务层校验） */
    private String result;
    /** 一句感受；空串=没写 */
    private String feeling;
    /** 盖章的人=对方；null=还没盖 */
    private String sealedBy;
    /** 盖章时间毫秒；null=还没盖 */
    private Long sealedAt;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestReport of(String spaceId, String battleId, String result, String feeling) {
        CoupleQuestReport row = new CoupleQuestReport();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.battleId = battleId;
        row.result = result;
        row.feeling = feeling == null ? "" : feeling;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 对方是否已盖章（sealed_by 非空非空白）。 */
    public boolean sealed() {
        return sealedBy != null && !sealedBy.isBlank();
    }

    /** 盖章：写盖章人与盖章毫秒，并刷新更新时间。 */
    public void seal(String by) {
        long now = System.currentTimeMillis();
        sealedBy = by;
        sealedAt = now;
        updatedAt = now;
    }
}
