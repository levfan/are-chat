package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineHomecookMapper extends BaseMapperCompat<CoupleDineHomecook> {

    /** 某人某周的拿手菜。 */
    default CoupleDineHomecook find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDineHomecook>()
                .eq(CoupleDineHomecook::getSpaceId, spaceId)
                .eq(CoupleDineHomecook::getWeek, week)
                .eq(CoupleDineHomecook::getFromUser, fromUser));
    }

    /** 某周双方各报的拿手菜。 */
    default List<CoupleDineHomecook> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleDineHomecook>()
                .eq(CoupleDineHomecook::getSpaceId, spaceId)
                .eq(CoupleDineHomecook::getWeek, week));
    }
}
