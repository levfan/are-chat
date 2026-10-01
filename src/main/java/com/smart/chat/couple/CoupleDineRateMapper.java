package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineRateMapper extends BaseMapperCompat<CoupleDineRate> {

    /** 空间的星评流水（最新在前）。 */
    default List<CoupleDineRate> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDineRate>()
                .eq(CoupleDineRate::getSpaceId, spaceId)
                .orderByDesc(CoupleDineRate::getCreated));
    }
}
