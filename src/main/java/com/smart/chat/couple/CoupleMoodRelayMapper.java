package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMoodRelayMapper extends BaseMapperCompat<CoupleMoodRelay> {

    /** 空间的接力记录（在路上在前）。 */
    default List<CoupleMoodRelay> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMoodRelay>()
                .eq(CoupleMoodRelay::getSpaceId, spaceId)
                .orderByAsc(CoupleMoodRelay::getStatus)
                .orderByDesc(CoupleMoodRelay::getCreated)
                .last("LIMIT 50"));
    }

    /** 空间正在路上的接力棒。 */
    default CoupleMoodRelay findPending(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CoupleMoodRelay>()
                .eq(CoupleMoodRelay::getSpaceId, spaceId)
                .eq(CoupleMoodRelay::getStatus, CoupleMoodRelay.STATUS_PENDING)
                .orderByDesc(CoupleMoodRelay::getCreated)
                .last("LIMIT 1"));
    }
}
