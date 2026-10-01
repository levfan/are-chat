package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F312 周期共览数据访问。 */
@Mapper
public interface CoupleBodyCycleMapper extends BaseMapperCompat<CoupleBodyCycle> {

    /** 当日本人一行（uk 保证最多一条）。 */
    default CoupleBodyCycle findByDayUser(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyCycle>()
                .eq(CoupleBodyCycle::getSpaceId, spaceId)
                .eq(CoupleBodyCycle::getDay, day)
                .eq(CoupleBodyCycle::getFromUser, fromUser));
    }

    /** fromDay 起的标记（旧→新，照顾卡回看用）。 */
    default List<CoupleBodyCycle> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleBodyCycle>()
                .eq(CoupleBodyCycle::getSpaceId, spaceId)
                .ge(CoupleBodyCycle::getDay, fromDay)
                .orderByAsc(CoupleBodyCycle::getDay));
    }
}
