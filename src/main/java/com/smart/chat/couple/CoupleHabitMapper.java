package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHabitMapper extends BaseMapperCompat<CoupleHabit> {

    /** 某空间全部习惯：进行中在前，按创建时间新→旧。 */
    default List<CoupleHabit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHabit>()
                .eq(CoupleHabit::getSpaceId, spaceId)
                .orderByDesc(CoupleHabit::getActive)
                .orderByDesc(CoupleHabit::getCreated));
    }
}
