package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCoolDownMapper extends BaseMapperCompat<CoupleCoolDown> {

    /** 空间的冷静角记录（进行中的在前，新的在前）。 */
    default List<CoupleCoolDown> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCoolDown>()
                .eq(CoupleCoolDown::getSpaceId, spaceId)
                .orderByAsc(CoupleCoolDown::getStatus)
                .orderByDesc(CoupleCoolDown::getCreated)
                .last("LIMIT 50"));
    }

    /** 空间当前进行中的冷静期。 */
    default CoupleCoolDown findActive(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CoupleCoolDown>()
                .eq(CoupleCoolDown::getSpaceId, spaceId)
                .eq(CoupleCoolDown::getStatus, CoupleCoolDown.STATUS_ACTIVE)
                .orderByDesc(CoupleCoolDown::getCreated)
                .last("LIMIT 1"));
    }
}
