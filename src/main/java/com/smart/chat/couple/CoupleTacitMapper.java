package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTacitMapper extends BaseMapperCompat<CoupleTacit> {

    /** 某空间全部对局（新→旧）。 */
    default List<CoupleTacit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTacit>()
                .eq(CoupleTacit::getSpaceId, spaceId)
                .orderByDesc(CoupleTacit::getCreated));
    }

    /** 进行中的一局（最多一局）。 */
    default CoupleTacit findPending(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CoupleTacit>()
                .eq(CoupleTacit::getSpaceId, spaceId)
                .isNull(CoupleTacit::getMatch)
                .orderByDesc(CoupleTacit::getCreated)
                .last("LIMIT 1"));
    }

    /** 默契一致的对局数。 */
    default long countMatched(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleTacit>()
                .eq(CoupleTacit::getSpaceId, spaceId)
                .eq(CoupleTacit::getMatch, 1));
    }
}
