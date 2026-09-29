package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleAnswerReactionMapper extends BaseMapperCompat<CoupleAnswerReaction> {

    /** 某人某天对 TA 回答的反应（每人每天至多一条）。 */
    default Optional<CoupleAnswerReaction> find(String spaceId, String answerDay, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleAnswerReaction>()
                        .eq(CoupleAnswerReaction::getSpaceId, spaceId)
                        .eq(CoupleAnswerReaction::getAnswerDay, answerDay)
                        .eq(CoupleAnswerReaction::getFromUser, fromUser))
                .stream().findFirst();
    }

    /** 某天双方的全部反应。 */
    default List<CoupleAnswerReaction> findByDay(String spaceId, String answerDay) {
        return selectList(new LambdaQueryWrapper<CoupleAnswerReaction>()
                .eq(CoupleAnswerReaction::getSpaceId, spaceId)
                .eq(CoupleAnswerReaction::getAnswerDay, answerDay)
                .orderByAsc(CoupleAnswerReaction::getCreated));
    }
}
