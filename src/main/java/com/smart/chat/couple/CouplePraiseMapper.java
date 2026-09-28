package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePraiseMapper extends BaseMapperCompat<CouplePraise> {

    /** 某空间全部夸夸卡（新→旧）。 */
    default List<CouplePraise> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePraise>()
                .eq(CouplePraise::getSpaceId, spaceId)
                .orderByDesc(CouplePraise::getCreated));
    }
}
