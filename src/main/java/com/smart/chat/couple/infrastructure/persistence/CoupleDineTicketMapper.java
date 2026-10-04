package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineTicketMapper extends BaseMapperCompat<CoupleDineTicket> {

    /** 某人某天的饭票。 */
    default CoupleDineTicket find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDineTicket>()
                .eq(CoupleDineTicket::getSpaceId, spaceId)
                .eq(CoupleDineTicket::getDay, day)
                .eq(CoupleDineTicket::getFromUser, fromUser));
    }

    /** 某天双方的饭票。 */
    default List<CoupleDineTicket> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleDineTicket>()
                .eq(CoupleDineTicket::getSpaceId, spaceId)
                .eq(CoupleDineTicket::getDay, day));
    }

    /** 空间全部饭票（攒裁决票池/年度账）。 */
    default List<CoupleDineTicket> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDineTicket>()
                .eq(CoupleDineTicket::getSpaceId, spaceId)
                .orderByDesc(CoupleDineTicket::getCreated));
    }
}
