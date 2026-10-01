package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F260 想被听时段数据访问。 */
@Mapper
public interface CoupleListenSlotMapper extends BaseMapperCompat<CoupleListenSlot> {

    /** 在途时段（OPEN/CONFIRMED）。 */
    default List<CoupleListenSlot> findCurrent(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleListenSlot>()
                .eq(CoupleListenSlot::getSpaceId, spaceId)
                .in(CoupleListenSlot::getStatus, CoupleListenSlot.STATUS_OPEN, CoupleListenSlot.STATUS_CONFIRMED)
                .orderByDesc(CoupleListenSlot::getCreated));
    }

    /** 某人某天是否已有申请。 */
    default List<CoupleListenSlot> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleListenSlot>()
                .eq(CoupleListenSlot::getSpaceId, spaceId)
                .eq(CoupleListenSlot::getDay, day));
    }
}
