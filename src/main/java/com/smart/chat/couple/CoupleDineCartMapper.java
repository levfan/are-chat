package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineCartMapper extends BaseMapperCompat<CoupleDineCart> {

    /** 某周搭伙车全部菜品。 */
    default List<CoupleDineCart> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleDineCart>()
                .eq(CoupleDineCart::getSpaceId, spaceId)
                .eq(CoupleDineCart::getWeek, week)
                .orderByAsc(CoupleDineCart::getCreated));
    }
}
