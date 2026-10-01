package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHandholdMapper extends BaseMapperCompat<CoupleHandhold> {

    /** 空间的牵手记录（新的在前）。 */
    default List<CoupleHandhold> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHandhold>()
                .eq(CoupleHandhold::getSpaceId, spaceId)
                .orderByDesc(CoupleHandhold::getDay)
                .last("LIMIT 60"));
    }

    /** 某天的牵手签到。 */
    default CoupleHandhold findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleHandhold>()
                .eq(CoupleHandhold::getSpaceId, spaceId)
                .eq(CoupleHandhold::getDay, day));
    }

    /** 双方都点亮的牵手天数（累计统计）。 */
    default long countBoth(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleHandhold>()
                .eq(CoupleHandhold::getSpaceId, spaceId)
                .eq(CoupleHandhold::getHoldA, 1)
                .eq(CoupleHandhold::getHoldB, 1));
    }
}
