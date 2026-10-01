package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHabitStreakMapper extends BaseMapperCompat<CoupleHabitStreak> {

    /** 空间内全部习惯（按创建倒序）。 */
    default List<CoupleHabitStreak> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHabitStreak>()
                .eq(CoupleHabitStreak::getSpaceId, spaceId)
                .orderByDesc(CoupleHabitStreak::getCreated));
    }

    /** 某人活跃中的习惯（同时只有一个）。 */
    default CoupleHabitStreak findOpenByUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleHabitStreak>()
                .eq(CoupleHabitStreak::getSpaceId, spaceId)
                .eq(CoupleHabitStreak::getFromUser, fromUser)
                .eq(CoupleHabitStreak::getStatus, CoupleHabitStreak.STATUS_OPEN)
                .orderByDesc(CoupleHabitStreak::getCreated)
                .last("LIMIT 1"));
    }
}
