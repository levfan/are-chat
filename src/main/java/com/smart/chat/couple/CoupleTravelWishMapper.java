package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTravelWishMapper extends BaseMapperCompat<CoupleTravelWish> {

    /** 空间的旅行心愿（心愿在前，去过在后，各按时间倒序）。 */
    default List<CoupleTravelWish> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTravelWish>()
                .eq(CoupleTravelWish::getSpaceId, spaceId)
                .orderByAsc(CoupleTravelWish::isVisited)
                .orderByDesc(CoupleTravelWish::getCreated));
    }
}
