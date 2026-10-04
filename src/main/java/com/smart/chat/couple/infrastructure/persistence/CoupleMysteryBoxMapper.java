package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMysteryBoxMapper extends BaseMapperCompat<CoupleMysteryBoxPO> {

    /** 空间的盲盒列表（新→旧）。 */
    default List<CoupleMysteryBoxPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMysteryBoxPO>()
                .eq(CoupleMysteryBoxPO::getSpaceId, spaceId)
                .orderByDesc(CoupleMysteryBoxPO::getCreated));
    }
}
