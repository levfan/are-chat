package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSelfContractMapper extends BaseMapperCompat<CoupleSelfContract> {

    /** 空间的契约（新的在前）。 */
    default List<CoupleSelfContract> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSelfContract>()
                .eq(CoupleSelfContract::getSpaceId, spaceId)
                .orderByDesc(CoupleSelfContract::getCreated)
                .last("LIMIT 30"));
    }
}
