package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHabitLogMapper extends BaseMapperCompat<CoupleHabitLog> {

    /** 某习惯全部打卡日志，按日期新→旧。 */
    default List<CoupleHabitLog> findByHabit(String habitId) {
        return selectList(new LambdaQueryWrapper<CoupleHabitLog>()
                .eq(CoupleHabitLog::getHabitId, habitId)
                .orderByDesc(CoupleHabitLog::getLogDay));
    }

    /** 某人某天的打卡记录。 */
    default CoupleHabitLog find(String habitId, String username, String logDay) {
        return selectOne(new LambdaQueryWrapper<CoupleHabitLog>()
                .eq(CoupleHabitLog::getHabitId, habitId)
                .eq(CoupleHabitLog::getUsername, username)
                .eq(CoupleHabitLog::getLogDay, logDay));
    }
}
