package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTicketMapper extends BaseMapperCompat<CoupleTicket> {

    /** 空间的票根（按观看日期新→旧）。 */
    default List<CoupleTicket> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTicket>()
                .eq(CoupleTicket::getSpaceId, spaceId)
                .orderByDesc(CoupleTicket::getWatchDay));
    }
}
