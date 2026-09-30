package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFortuneSlipMapper extends BaseMapperCompat<CoupleFortuneSlip> {

    /** 某人某天已抽的签（每人每天一支，可覆盖重抽）。 */
    default CoupleFortuneSlip find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFortuneSlip>()
                .eq(CoupleFortuneSlip::getSpaceId, spaceId)
                .eq(CoupleFortuneSlip::getFromUser, fromUser)
                .eq(CoupleFortuneSlip::getDay, day));
    }

    /** 最近抽过的签（新→旧）。 */
    default List<CoupleFortuneSlip> findBySpace(String spaceId, int limit) {
        return selectList(new LambdaQueryWrapper<CoupleFortuneSlip>()
                .eq(CoupleFortuneSlip::getSpaceId, spaceId)
                .orderByDesc(CoupleFortuneSlip::getCreated))
                .stream()
                .limit(limit)
                .toList();
    }
}
