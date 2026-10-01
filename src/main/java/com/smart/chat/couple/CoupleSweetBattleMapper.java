package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSweetBattleMapper extends BaseMapperCompat<CoupleSweetBattle> {

    /** 某天的擂台。 */
    default CoupleSweetBattle findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleSweetBattle>()
                .eq(CoupleSweetBattle::getSpaceId, spaceId)
                .eq(CoupleSweetBattle::getDay, day));
    }

    /** 空间的历史擂台（新的在前）。 */
    default List<CoupleSweetBattle> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSweetBattle>()
                .eq(CoupleSweetBattle::getSpaceId, spaceId)
                .orderByDesc(CoupleSweetBattle::getDay)
                .last("LIMIT 14"));
    }
}
