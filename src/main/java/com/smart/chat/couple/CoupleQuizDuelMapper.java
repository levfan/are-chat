package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleQuizDuelMapper extends BaseMapperCompat<CoupleQuizDuel> {

    /** 空间的出题（新的在前）。 */
    default List<CoupleQuizDuel> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuizDuel>()
                .eq(CoupleQuizDuel::getSpaceId, spaceId)
                .orderByDesc(CoupleQuizDuel::getCreated)
                .last("LIMIT 30"));
    }
}
