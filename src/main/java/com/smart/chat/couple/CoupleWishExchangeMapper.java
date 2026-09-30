package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleWishExchangeMapper extends BaseMapperCompat<CoupleWishExchange> {

    /** 空间的心愿互换（新→旧）。 */
    default List<CoupleWishExchange> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWishExchange>()
                .eq(CoupleWishExchange::getSpaceId, spaceId)
                .orderByDesc(CoupleWishExchange::getCreated));
    }
}
