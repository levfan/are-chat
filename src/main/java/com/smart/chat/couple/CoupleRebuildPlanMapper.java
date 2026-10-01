package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F323 信任重建计划数据访问。 */
@Mapper
public interface CoupleRebuildPlanMapper extends BaseMapperCompat<CoupleRebuildPlan> {

    /** 在营中的计划（起始日升序）。 */
    default List<CoupleRebuildPlan> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRebuildPlan>()
                .eq(CoupleRebuildPlan::getSpaceId, spaceId)
                .eq(CoupleRebuildPlan::getStatus, CoupleRebuildPlan.STATUS_OPEN)
                .orderByAsc(CoupleRebuildPlan::getStartDay));
    }

    /** 同空间的同名计划（uk 保证最多一条）。 */
    default CoupleRebuildPlan findName(String spaceId, String name) {
        return selectOne(new LambdaQueryWrapper<CoupleRebuildPlan>()
                .eq(CoupleRebuildPlan::getSpaceId, spaceId)
                .eq(CoupleRebuildPlan::getName, name));
    }

    /** 空间全部计划（新→旧）。 */
    default List<CoupleRebuildPlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRebuildPlan>()
                .eq(CoupleRebuildPlan::getSpaceId, spaceId)
                .orderByDesc(CoupleRebuildPlan::getCreated));
    }
}
