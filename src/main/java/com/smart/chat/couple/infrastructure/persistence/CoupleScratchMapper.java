package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleScratchMapper extends BaseMapperCompat<CoupleScratchPO> {

    /** 某周的全部刮刮乐（双方各一张）。 */
    default List<CoupleScratchPO> findByWeek(String spaceId, String weekKey) {
        return selectList(new LambdaQueryWrapper<CoupleScratchPO>()
                .eq(CoupleScratchPO::getSpaceId, spaceId)
                .eq(CoupleScratchPO::getWeekKey, weekKey));
    }

    /** 某人自己的刮刮乐（新→旧）。 */
    default List<CoupleScratchPO> findByOwner(String spaceId, String owner) {
        return selectList(new LambdaQueryWrapper<CoupleScratchPO>()
                .eq(CoupleScratchPO::getSpaceId, spaceId)
                .eq(CoupleScratchPO::getOwner, owner)
                .orderByDesc(CoupleScratchPO::getCreated));
    }

    /** 空间里全部刮刮乐（新→旧，用于统计）。 */
    default List<CoupleScratchPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleScratchPO>()
                .eq(CoupleScratchPO::getSpaceId, spaceId)
                .orderByDesc(CoupleScratchPO::getCreated));
    }
}
