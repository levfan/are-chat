package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSorryTicketMapper extends BaseMapperCompat<CoupleSorryTicket> {

    /** 空间的道歉券（新→旧）。 */
    default List<CoupleSorryTicket> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSorryTicket>()
                .eq(CoupleSorryTicket::getSpaceId, spaceId)
                .orderByDesc(CoupleSorryTicket::getCreated));
    }

    /** 某人有效的道歉券。 */
    default List<CoupleSorryTicket> findActiveByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleSorryTicket>()
                .eq(CoupleSorryTicket::getSpaceId, spaceId)
                .eq(CoupleSorryTicket::getFromUser, fromUser)
                .eq(CoupleSorryTicket::getStatus, CoupleSorryTicket.STATUS_ACTIVE));
    }
}
