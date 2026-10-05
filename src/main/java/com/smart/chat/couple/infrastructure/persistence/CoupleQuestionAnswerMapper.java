package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleQuestionAnswerMapper extends BaseMapperCompat<CoupleQuestionAnswerPO> {

    /** 某人某天的回答（没答返回空）。 */
    default java.util.Optional<CoupleQuestionAnswerPO> find(String spaceId, String day, String username) {
        return selectList(new LambdaQueryWrapper<CoupleQuestionAnswerPO>()
                .eq(CoupleQuestionAnswerPO::getSpaceId, spaceId)
                .eq(CoupleQuestionAnswerPO::getDay, day)
                .eq(CoupleQuestionAnswerPO::getUsername, username)
                .last("LIMIT 1")).stream().findFirst();
    }

    /** 某天双方的全部回答（最多两行）。 */
    default List<CoupleQuestionAnswerPO> findBySpaceAndDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleQuestionAnswerPO>()
                .eq(CoupleQuestionAnswerPO::getSpaceId, spaceId)
                .eq(CoupleQuestionAnswerPO::getDay, day));
    }

    /** 某空间从 fromDay（含）起的回答，按日期倒序，用于回看与百日回顾。 */
    default List<CoupleQuestionAnswerPO> findBySpaceFrom(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleQuestionAnswerPO>()
                .eq(CoupleQuestionAnswerPO::getSpaceId, spaceId)
                .ge(CoupleQuestionAnswerPO::getDay, fromDay)
                .orderByDesc(CoupleQuestionAnswerPO::getDay));
    }

    /** 某空间全部回答（倒序）。 */
    default List<CoupleQuestionAnswerPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestionAnswerPO>()
                .eq(CoupleQuestionAnswerPO::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestionAnswerPO::getDay));
    }

    /** 全库累计回答条数。 */
    default long countAll() {
        return selectCount(new LambdaQueryWrapper<CoupleQuestionAnswerPO>());
    }
}