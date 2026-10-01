package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSafetyPingMapper extends BaseMapperCompat<CoupleSafetyPing> {

    /** 空间的平安卡（新的在前）。 */
    default List<CoupleSafetyPing> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSafetyPing>()
                .eq(CoupleSafetyPing::getSpaceId, spaceId)
                .orderByDesc(CoupleSafetyPing::getCreated)
                .last("LIMIT 30"));
    }
}
