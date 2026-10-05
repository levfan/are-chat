package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleStreakDayMapper extends BaseMapperCompat<CoupleStreakDayPO> {

    /** 某空间全部打卡日（升序，连续段算法要按日子顺序走）。 */
    default List<CoupleStreakDayPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleStreakDayPO>()
                .eq(CoupleStreakDayPO::getSpaceId, spaceId)
                .orderByAsc(CoupleStreakDayPO::getDay));
    }

    /** 某一天是否已确认打卡。 */
    default CoupleStreakDayPO find(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleStreakDayPO>()
                .eq(CoupleStreakDayPO::getSpaceId, spaceId)
                .eq(CoupleStreakDayPO::getDay, day)
                .last("LIMIT 1"));
    }

    /** 全库累计打卡天数。 */
    default long countAll() {
        return selectCount(new LambdaQueryWrapper<CoupleStreakDayPO>());
    }

    /** 某个日期区间内该空间的补签条数（yyyy-MM-dd 字典序即时间序）。 */
    default long countMakeupBetween(String spaceId, String fromDay, String toDay) {
        return selectCount(new LambdaQueryWrapper<CoupleStreakDayPO>()
                .eq(CoupleStreakDayPO::getSpaceId, spaceId)
                .eq(CoupleStreakDayPO::getSource, CoupleStreakDayPO.SOURCE_MAKEUP)
                .ge(CoupleStreakDayPO::getDay, fromDay)
                .le(CoupleStreakDayPO::getDay, toDay));
    }
}
