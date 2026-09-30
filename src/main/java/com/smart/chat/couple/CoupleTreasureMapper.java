package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTreasureMapper extends BaseMapperCompat<CoupleTreasure> {

    /** 空间的藏宝图（新→旧）。 */
    default List<CoupleTreasure> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTreasure>()
                .eq(CoupleTreasure::getSpaceId, spaceId)
                .orderByDesc(CoupleTreasure::getCreated));
    }

    /** 某人埋的未揭晓宝藏（每人同时最多一个）。 */
    default CoupleTreasure findPendingByUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTreasure>()
                .eq(CoupleTreasure::getSpaceId, spaceId)
                .eq(CoupleTreasure::getFromUser, fromUser)
                .eq(CoupleTreasure::getStatus, CoupleTreasure.STATUS_PENDING)
                .last("LIMIT 1"));
    }
}
