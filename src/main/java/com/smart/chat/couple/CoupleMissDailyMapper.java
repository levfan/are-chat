package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMissDailyMapper extends BaseMapperCompat<CoupleMissDaily> {

    /** 空间的想念记录（新的在前）。 */
    default List<CoupleMissDaily> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMissDaily>()
                .eq(CoupleMissDaily::getSpaceId, spaceId)
                .orderByDesc(CoupleMissDaily::getDay)
                .last("LIMIT 60"));
    }

    /** 某天的想念签到。 */
    default CoupleMissDaily findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleMissDaily>()
                .eq(CoupleMissDaily::getSpaceId, spaceId)
                .eq(CoupleMissDaily::getDay, day));
    }

    /** 双向奔赴天数（累计统计）。 */
    default long countBoth(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleMissDaily>()
                .eq(CoupleMissDaily::getSpaceId, spaceId)
                .eq(CoupleMissDaily::getMissA, 1)
                .eq(CoupleMissDaily::getMissB, 1));
    }
}
