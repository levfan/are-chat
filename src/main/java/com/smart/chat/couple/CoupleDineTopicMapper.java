package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineTopicMapper extends BaseMapperCompat<CoupleDineTopic> {

    /** 某天的话题卡标记。 */
    default CoupleDineTopic find(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleDineTopic>()
                .eq(CoupleDineTopic::getSpaceId, spaceId)
                .eq(CoupleDineTopic::getDay, day));
    }

    default List<CoupleDineTopic> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDineTopic>()
                .eq(CoupleDineTopic::getSpaceId, spaceId));
    }
}
