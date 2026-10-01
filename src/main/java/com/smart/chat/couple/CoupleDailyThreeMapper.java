package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDailyThreeMapper extends BaseMapperCompat<CoupleDailyThree> {

    /** 某天的三问作答。 */
    default List<CoupleDailyThree> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleDailyThree>()
                .eq(CoupleDailyThree::getSpaceId, spaceId)
                .eq(CoupleDailyThree::getDay, day));
    }

    /** 某人某天的作答。 */
    default CoupleDailyThree find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleDailyThree>()
                .eq(CoupleDailyThree::getSpaceId, spaceId)
                .eq(CoupleDailyThree::getFromUser, fromUser)
                .eq(CoupleDailyThree::getDay, day));
    }
}
