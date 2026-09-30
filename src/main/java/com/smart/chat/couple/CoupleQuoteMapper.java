package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleQuoteMapper extends BaseMapperCompat<CoupleQuote> {

    /** 空间的语录（新→旧）。 */
    default List<CoupleQuote> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuote>()
                .eq(CoupleQuote::getSpaceId, spaceId)
                .orderByDesc(CoupleQuote::getCreated));
    }
}
