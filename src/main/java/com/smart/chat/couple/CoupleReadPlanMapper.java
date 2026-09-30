package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleReadPlanMapper extends BaseMapperCompat<CoupleReadPlan> {

    /** 空间的共读计划（新→旧）。 */
    default List<CoupleReadPlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleReadPlan>()
                .eq(CoupleReadPlan::getSpaceId, spaceId)
                .orderByDesc(CoupleReadPlan::getCreated));
    }
}
