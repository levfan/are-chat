package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePartnerFactMapper extends BaseMapperCompat<CouplePartnerFact> {

    /** 空间的手册条目（新的在前）。 */
    default List<CouplePartnerFact> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePartnerFact>()
                .eq(CouplePartnerFact::getSpaceId, spaceId)
                .orderByDesc(CouplePartnerFact::getCreated)
                .last("LIMIT 60"));
    }
}
