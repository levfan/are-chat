package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 经期记录行：一行 = 一次经期，space_id + start_day 唯一。
 * 平均周期与下次预测不落列，读时由 {@code PeriodCycles} 从 start_day 集合算。
 */
@Data
@TableName("couple_period")
public class CouplePeriodPO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String startDay;
    private String endDay;
    private String recorderUser;
    private Long created;
    private Long updatedAt;
}
