package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleVisionCardMapper extends BaseMapperCompat<CoupleVisionCard> {

    /** 空间的愿景卡（新的在前）。 */
    default List<CoupleVisionCard> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleVisionCard>()
                .eq(CoupleVisionCard::getSpaceId, spaceId)
                .orderByDesc(CoupleVisionCard::getCreated)
                .last("LIMIT 50"));
    }
}
