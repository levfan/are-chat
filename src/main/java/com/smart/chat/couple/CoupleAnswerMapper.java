package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleAnswerMapper extends BaseMapperCompat<CoupleAnswer> {

    /** 某空间某天某人的回答（用于判断是否已答/更新）。 */
    default Optional<CoupleAnswer> find(String spaceId, String day, String username) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleAnswer>()
                .eq(CoupleAnswer::getSpaceId, spaceId)
                .eq(CoupleAnswer::getAnswerDay, day)
                .eq(CoupleAnswer::getUsername, username)
                .last("LIMIT 1")));
    }

    /** 某空间某天的全部回答（双方拼在一起看）。 */
    default List<CoupleAnswer> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleAnswer>()
                .eq(CoupleAnswer::getSpaceId, spaceId)
                .eq(CoupleAnswer::getAnswerDay, day));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleAnswer>().eq(CoupleAnswer::getSpaceId, spaceId));
    }
}
