package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F287 习惯图鉴数据访问。 */
@Mapper
public interface CoupleHabitMapMapper extends BaseMapperCompat<CoupleHabitMap> {

    /** 同观察员同习惯一条。 */
    default CoupleHabitMap findHabit(String spaceId, String habit, String observerUser) {
        return selectOne(new LambdaQueryWrapper<CoupleHabitMap>()
                .eq(CoupleHabitMap::getSpaceId, spaceId)
                .eq(CoupleHabitMap::getHabit, habit)
                .eq(CoupleHabitMap::getObserverUser, observerUser));
    }

    /** 全部图鉴（新→旧）。 */
    default List<CoupleHabitMap> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHabitMap>()
                .eq(CoupleHabitMap::getSpaceId, spaceId)
                .orderByDesc(CoupleHabitMap::getCreated));
    }
}
