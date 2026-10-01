package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSoulAnswerMapper extends BaseMapperCompat<CoupleSoulAnswer> {

    /** 某人某天的作答。 */
    default CoupleSoulAnswer find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleSoulAnswer>()
                .eq(CoupleSoulAnswer::getSpaceId, spaceId)
                .eq(CoupleSoulAnswer::getDay, day)
                .eq(CoupleSoulAnswer::getFromUser, fromUser));
    }

    /** 全部作答（按创建倒序）。 */
    default List<CoupleSoulAnswer> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSoulAnswer>()
                .eq(CoupleSoulAnswer::getSpaceId, spaceId)
                .orderByDesc(CoupleSoulAnswer::getCreated));
    }
}
