package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F310 体征互报数据访问。 */
@Mapper
public interface CoupleBodyMetricMapper extends BaseMapperCompat<CoupleBodyMetric> {

    /** 当日两人行（A 在前）。 */
    default List<CoupleBodyMetric> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleBodyMetric>()
                .eq(CoupleBodyMetric::getSpaceId, spaceId)
                .eq(CoupleBodyMetric::getDay, day)
                .orderByAsc(CoupleBodyMetric::getFromUser));
    }

    /** 当日本人一行（uk 保证最多一条）。 */
    default CoupleBodyMetric findByDayUser(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyMetric>()
                .eq(CoupleBodyMetric::getSpaceId, spaceId)
                .eq(CoupleBodyMetric::getDay, day)
                .eq(CoupleBodyMetric::getFromUser, fromUser));
    }

    /** 区间趋势用：本人 fromDay 起的记录（旧→新）。 */
    default List<CoupleBodyMetric> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleBodyMetric>()
                .eq(CoupleBodyMetric::getSpaceId, spaceId)
                .ge(CoupleBodyMetric::getDay, fromDay)
                .orderByAsc(CoupleBodyMetric::getDay));
    }
}
