package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F268 休战旗数据访问。 */
@Mapper
public interface CoupleTruceMapper extends BaseMapperCompat<CoupleTruce> {

    /** 在途休战旗（最多一面）。 */
    default List<CoupleTruce> findCurrent(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTruce>()
                .eq(CoupleTruce::getSpaceId, spaceId)
                .eq(CoupleTruce::getStatus, CoupleTruce.STATUS_ON)
                .orderByDesc(CoupleTruce::getCreated));
    }

    /** 近况列表（新→旧）。 */
    default List<CoupleTruce> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTruce>()
                .eq(CoupleTruce::getSpaceId, spaceId)
                .orderByDesc(CoupleTruce::getCreated));
    }
}
