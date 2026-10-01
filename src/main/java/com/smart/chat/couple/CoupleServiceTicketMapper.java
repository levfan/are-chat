package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F308 今日客服工单数据访问。 */
@Mapper
public interface CoupleServiceTicketMapper extends BaseMapperCompat<CoupleServiceTicket> {

    default List<CoupleServiceTicket> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleServiceTicket>()
                .eq(CoupleServiceTicket::getSpaceId, spaceId)
                .orderByDesc(CoupleServiceTicket::getCreated));
    }

    /** 在途工单（未评分前都算待办）。 */
    default List<CoupleServiceTicket> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleServiceTicket>()
                .eq(CoupleServiceTicket::getSpaceId, spaceId)
                .in(CoupleServiceTicket::getStatus,
                        CoupleServiceTicket.STATUS_OPEN, CoupleServiceTicket.STATUS_ANSWERED,
                        CoupleServiceTicket.STATUS_APPEALED));
    }
}
