package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleOathMapper extends BaseMapperCompat<CoupleOath> {

    /** 空间的承诺（新的在前）。 */
    default List<CoupleOath> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleOath>()
                .eq(CoupleOath::getSpaceId, spaceId)
                .orderByDesc(CoupleOath::getCreated)
                .last("LIMIT 50"));
    }
}
