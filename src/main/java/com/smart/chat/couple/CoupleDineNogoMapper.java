package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineNogoMapper extends BaseMapperCompat<CoupleDineNogo> {

    /** 按店名查踩雷记录（同空间同名只留一条）。 */
    default CoupleDineNogo find(String spaceId, String name) {
        return selectOne(new LambdaQueryWrapper<CoupleDineNogo>()
                .eq(CoupleDineNogo::getSpaceId, spaceId)
                .eq(CoupleDineNogo::getName, name));
    }

    default List<CoupleDineNogo> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDineNogo>()
                .eq(CoupleDineNogo::getSpaceId, spaceId)
                .orderByDesc(CoupleDineNogo::getCreated));
    }
}
