package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F324 和好了倒计时数据访问。 */
@Mapper
public interface CoupleRepairMakeupMapper extends BaseMapperCompat<CoupleRepairMakeup> {

    /** 某天的倒计时（uk 保证最多一条）。 */
    default CoupleRepairMakeup findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleRepairMakeup>()
                .eq(CoupleRepairMakeup::getSpaceId, spaceId)
                .eq(CoupleRepairMakeup::getDay, day));
    }

    /** 还在走的倒计时。 */
    default List<CoupleRepairMakeup> findRunning(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRepairMakeup>()
                .eq(CoupleRepairMakeup::getSpaceId, spaceId)
                .eq(CoupleRepairMakeup::getStatus, CoupleRepairMakeup.STATUS_RUNNING));
    }

    /** 空间全部倒计时（冷战日新→旧）。 */
    default List<CoupleRepairMakeup> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRepairMakeup>()
                .eq(CoupleRepairMakeup::getSpaceId, spaceId)
                .orderByDesc(CoupleRepairMakeup::getDay));
    }
}
