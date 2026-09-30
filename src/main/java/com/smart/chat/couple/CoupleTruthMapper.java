package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTruthMapper extends BaseMapperCompat<CoupleTruth> {

    /** 某天某题的全部回答。 */
    default List<CoupleTruth> findByDay(String spaceId, String day, String question) {
        return selectList(new LambdaQueryWrapper<CoupleTruth>()
                .eq(CoupleTruth::getSpaceId, spaceId)
                .eq(CoupleTruth::getDay, day)
                .eq(CoupleTruth::getQuestion, question));
    }

    /** 空间全部真心话（新→旧）。 */
    default List<CoupleTruth> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTruth>()
                .eq(CoupleTruth::getSpaceId, spaceId)
                .orderByDesc(CoupleTruth::getCreated));
    }
}
