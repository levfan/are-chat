package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleMoodReactionMapper extends BaseMapperCompat<CoupleMoodReactionPO> {

    /** 某人某天对对方心情的回应（无则空）。 */
    default CoupleMoodReactionPO find(String spaceId, String moodDay, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleMoodReactionPO>()
                .eq(CoupleMoodReactionPO::getSpaceId, spaceId)
                .eq(CoupleMoodReactionPO::getMoodDay, moodDay)
                .eq(CoupleMoodReactionPO::getFromUser, fromUser));
    }
}
