package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineTicketMapper extends BaseMapperCompat<CoupleDineTicketPO> {

    /** 某人某天的饭票。 */
    default CoupleDineTicketPO find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDineTicketPO>()
                .eq(CoupleDineTicketPO::getSpaceId, spaceId)
                .eq(CoupleDineTicketPO::getDay, day)
                .eq(CoupleDineTicketPO::getFromUser, fromUser));
    }

    /** 某天双方的饭票。 */
    default List<CoupleDineTicketPO> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleDineTicketPO>()
                .eq(CoupleDineTicketPO::getSpaceId, spaceId)
                .eq(CoupleDineTicketPO::getDay, day));
    }

    /** 空间全部饭票（攒裁决票池/年度账）。 */
    default List<CoupleDineTicketPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDineTicketPO>()
                .eq(CoupleDineTicketPO::getSpaceId, spaceId)
                .orderByDesc(CoupleDineTicketPO::getCreated));
    }
}
