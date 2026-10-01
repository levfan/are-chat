package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePraiseBankMapper extends BaseMapperCompat<CouplePraiseBank> {

    /** 全部优点存款（按创建倒序）。 */
    default List<CouplePraiseBank> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePraiseBank>()
                .eq(CouplePraiseBank::getSpaceId, spaceId)
                .orderByDesc(CouplePraiseBank::getCreated));
    }
}
