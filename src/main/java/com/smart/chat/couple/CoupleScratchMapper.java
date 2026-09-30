package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleScratchMapper extends BaseMapperCompat<CoupleScratch> {

    /** 某周的全部刮刮乐（双方各一张）。 */
    default List<CoupleScratch> findByWeek(String spaceId, String weekKey) {
        return selectList(new LambdaQueryWrapper<CoupleScratch>()
                .eq(CoupleScratch::getSpaceId, spaceId)
                .eq(CoupleScratch::getWeekKey, weekKey));
    }

    /** 某人自己的刮刮乐（新→旧）。 */
    default List<CoupleScratch> findByOwner(String spaceId, String owner) {
        return selectList(new LambdaQueryWrapper<CoupleScratch>()
                .eq(CoupleScratch::getSpaceId, spaceId)
                .eq(CoupleScratch::getOwner, owner)
                .orderByDesc(CoupleScratch::getCreated));
    }

    /** 空间里全部刮刮乐（新→旧，用于统计）。 */
    default List<CoupleScratch> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleScratch>()
                .eq(CoupleScratch::getSpaceId, spaceId)
                .orderByDesc(CoupleScratch::getCreated));
    }
}
