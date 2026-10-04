package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleBondDayMapper extends BaseMapperCompat<CoupleBondDayPO> {

    /** 某空间全部打卡日（升序，连续段算法要按日子顺序走）。 */
    default List<CoupleBondDayPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBondDayPO>()
                .eq(CoupleBondDayPO::getSpaceId, spaceId)
                .orderByAsc(CoupleBondDayPO::getDay));
    }

    /** 某一天是否已确认打卡。 */
    default CoupleBondDayPO find(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleBondDayPO>()
                .eq(CoupleBondDayPO::getSpaceId, spaceId)
                .eq(CoupleBondDayPO::getDay, day)
                .last("LIMIT 1"));
    }

    /** 某个日期区间内该空间的补签条数（yyyy-MM-dd 字典序即时间序）。 */
    default long countMakeupBetween(String spaceId, String fromDay, String toDay) {
        return selectCount(new LambdaQueryWrapper<CoupleBondDayPO>()
                .eq(CoupleBondDayPO::getSpaceId, spaceId)
                .eq(CoupleBondDayPO::getSource, CoupleBondDayPO.SOURCE_MAKEUP)
                .ge(CoupleBondDayPO::getDay, fromDay)
                .le(CoupleBondDayPO::getDay, toDay));
    }
}
