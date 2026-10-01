package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDriftBottleMapper extends BaseMapperCompat<CoupleDriftBottle> {

    /** 全部漂流瓶（漂着的在前，按创建倒序）。 */
    default List<CoupleDriftBottle> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDriftBottle>()
                .eq(CoupleDriftBottle::getSpaceId, spaceId)
                .orderByDesc(CoupleDriftBottle::getCreated));
    }
}
