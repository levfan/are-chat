package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleAnnivPlanMapper extends BaseMapperCompat<CoupleAnnivPlan> {

    /** 全部策划案（按纪念日日期正序）。 */
    default List<CoupleAnnivPlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAnnivPlan>()
                .eq(CoupleAnnivPlan::getSpaceId, spaceId)
                .orderByAsc(CoupleAnnivPlan::getDay));
    }
}
