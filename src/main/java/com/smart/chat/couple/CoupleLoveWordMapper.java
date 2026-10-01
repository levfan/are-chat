package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleLoveWordMapper extends BaseMapperCompat<CoupleLoveWord> {

    /** 空间收藏的情话（新的在前）。 */
    default List<CoupleLoveWord> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLoveWord>()
                .eq(CoupleLoveWord::getSpaceId, spaceId)
                .orderByDesc(CoupleLoveWord::getCreated)
                .last("LIMIT 30"));
    }
}
