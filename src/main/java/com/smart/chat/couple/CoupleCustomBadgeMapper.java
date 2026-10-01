package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCustomBadgeMapper extends BaseMapperCompat<CoupleCustomBadge> {

    /** 空间的成就墙（新的在前）。 */
    default List<CoupleCustomBadge> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCustomBadge>()
                .eq(CoupleCustomBadge::getSpaceId, spaceId)
                .orderByDesc(CoupleCustomBadge::getCreated)
                .last("LIMIT 30"));
    }
}
