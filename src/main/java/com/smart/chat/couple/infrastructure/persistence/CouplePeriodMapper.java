package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePeriodMapper extends BaseMapperCompat<CouplePeriodPO> {

    /** 某空间的全部经期记录：开始日倒序（yyyy-MM-dd 定长字符串，字典序即日期序）。 */
    default List<CouplePeriodPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePeriodPO>()
                .eq(CouplePeriodPO::getSpaceId, spaceId)
                .orderByDesc(CouplePeriodPO::getStartDay));
    }

    default boolean existsByStart(String spaceId, String startDay) {
        return selectCount(new LambdaQueryWrapper<CouplePeriodPO>()
                .eq(CouplePeriodPO::getSpaceId, spaceId)
                .eq(CouplePeriodPO::getStartDay, startDay)) > 0;
    }
}
