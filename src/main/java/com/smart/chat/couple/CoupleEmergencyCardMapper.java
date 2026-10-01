package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleEmergencyCardMapper extends BaseMapperCompat<CoupleEmergencyCard> {

    /** 某人填的应急卡。 */
    default CoupleEmergencyCard find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleEmergencyCard>()
                .eq(CoupleEmergencyCard::getSpaceId, spaceId)
                .eq(CoupleEmergencyCard::getFromUser, fromUser));
    }
}
