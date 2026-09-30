package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleApologyCardMapper extends BaseMapperCompat<CoupleApologyCard> {

    /** 空间的道歉卡（待收下的在前）。 */
    default List<CoupleApologyCard> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleApologyCard>()
                .eq(CoupleApologyCard::getSpaceId, spaceId)
                .orderByAsc(CoupleApologyCard::getStatus)
                .orderByDesc(CoupleApologyCard::getCreated)
                .last("LIMIT 50"));
    }
}
