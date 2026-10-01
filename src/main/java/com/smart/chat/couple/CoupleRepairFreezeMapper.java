package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F320 冷冻解冻规程数据访问。 */
@Mapper
public interface CoupleRepairFreezeMapper extends BaseMapperCompat<CoupleRepairFreeze> {

    /** 某日挂出的冻结单（uk 保证最多一条）。 */
    default CoupleRepairFreeze find(String spaceId, String startDay) {
        return selectOne(new LambdaQueryWrapper<CoupleRepairFreeze>()
                .eq(CoupleRepairFreeze::getSpaceId, spaceId)
                .eq(CoupleRepairFreeze::getStartDay, startDay));
    }

    /** 当前在冻的一单（挂冻结日最新的一条，可能多单未解冻）。 */
    default CoupleRepairFreeze findCurrent(String spaceId) {
        List<CoupleRepairFreeze> rows = selectList(new LambdaQueryWrapper<CoupleRepairFreeze>()
                .eq(CoupleRepairFreeze::getSpaceId, spaceId)
                .eq(CoupleRepairFreeze::getStatus, CoupleRepairFreeze.STATUS_FROZEN)
                .orderByDesc(CoupleRepairFreeze::getStartDay));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 空间全部冻结单（挂冻结日新→旧）。 */
    default List<CoupleRepairFreeze> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRepairFreeze>()
                .eq(CoupleRepairFreeze::getSpaceId, spaceId)
                .orderByDesc(CoupleRepairFreeze::getStartDay));
    }
}
