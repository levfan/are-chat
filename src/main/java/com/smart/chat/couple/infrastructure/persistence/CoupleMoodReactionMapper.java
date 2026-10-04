package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleMoodReactionMapper extends BaseMapperCompat<CoupleMoodReaction> {

    /** 某人某天对对方心情的回应（无则空）。 */
    default CoupleMoodReaction find(String spaceId, String moodDay, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleMoodReaction>()
                .eq(CoupleMoodReaction::getSpaceId, spaceId)
                .eq(CoupleMoodReaction::getMoodDay, moodDay)
                .eq(CoupleMoodReaction::getFromUser, fromUser));
    }
}
