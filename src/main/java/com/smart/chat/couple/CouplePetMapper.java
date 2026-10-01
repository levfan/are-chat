package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouplePetMapper extends BaseMapperCompat<CouplePet> {

    /** 空间的守护兽（每空间一只）。 */
    default CouplePet findBySpace(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CouplePet>()
                .eq(CouplePet::getSpaceId, spaceId));
    }
}
