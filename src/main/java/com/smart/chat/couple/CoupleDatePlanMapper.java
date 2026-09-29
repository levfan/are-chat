package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDatePlanMapper extends BaseMapperCompat<CoupleDatePlan> {

    /** 某空间全部约会：计划中按日期升序在前，已完成归档在后。 */
    default List<CoupleDatePlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDatePlan>()
                .eq(CoupleDatePlan::getSpaceId, spaceId)
                .orderByAsc(CoupleDatePlan::getPlanDay));
    }
}
