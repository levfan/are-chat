package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFiveYearPlanMapper extends BaseMapperCompat<CoupleFiveYearPlan> {

    /** 全部五年之约（按创建正序）。 */
    default List<CoupleFiveYearPlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFiveYearPlan>()
                .eq(CoupleFiveYearPlan::getSpaceId, spaceId)
                .orderByAsc(CoupleFiveYearPlan::getCreated));
    }
}
