package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDreamMapper extends BaseMapperCompat<CoupleDream> {

    /** 空间的梦境手账（新的在前）。 */
    default List<CoupleDream> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDream>()
                .eq(CoupleDream::getSpaceId, spaceId)
                .orderByDesc(CoupleDream::getCreated)
                .last("LIMIT 30"));
    }
}
