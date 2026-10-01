package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F319 早睡军令状数据访问。 */
@Mapper
public interface CoupleBodyOathMapper extends BaseMapperCompat<CoupleBodyOath> {

    /** 本周状子一行（uk 保证最多一条）。 */
    default CoupleBodyOath findByWeek(String spaceId, String week) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyOath>()
                .eq(CoupleBodyOath::getSpaceId, spaceId)
                .eq(CoupleBodyOath::getWeek, week));
    }

    /** fromWeek 起的状子（旧→新，违约率回看用）。 */
    default List<CoupleBodyOath> findRecent(String spaceId, String fromWeek) {
        return selectList(new LambdaQueryWrapper<CoupleBodyOath>()
                .eq(CoupleBodyOath::getSpaceId, spaceId)
                .ge(CoupleBodyOath::getWeek, fromWeek)
                .orderByAsc(CoupleBodyOath::getWeek));
    }
}
