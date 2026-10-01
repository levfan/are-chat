package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePoemChainMapper extends BaseMapperCompat<CouplePoemChain> {

    /** 全部诗句（按创建正序，保持诗的顺序）。 */
    default List<CouplePoemChain> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePoemChain>()
                .eq(CouplePoemChain::getSpaceId, spaceId)
                .orderByAsc(CouplePoemChain::getCreated));
    }

    /** 某人某天的诗句。 */
    default CouplePoemChain find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CouplePoemChain>()
                .eq(CouplePoemChain::getSpaceId, spaceId)
                .eq(CouplePoemChain::getDay, day)
                .eq(CouplePoemChain::getFromUser, fromUser));
    }
}
