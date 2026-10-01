package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F263 卡壳一问数据访问。 */
@Mapper
public interface CoupleStuckQMapper extends BaseMapperCompat<CoupleStuckQ> {

    /** 某人本周的一题。 */
    default CoupleStuckQ find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleStuckQ>()
                .eq(CoupleStuckQ::getSpaceId, spaceId)
                .eq(CoupleStuckQ::getWeek, week)
                .eq(CoupleStuckQ::getFromUser, fromUser));
    }

    /** 本周双题。 */
    default List<CoupleStuckQ> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleStuckQ>()
                .eq(CoupleStuckQ::getSpaceId, spaceId)
                .eq(CoupleStuckQ::getWeek, week));
    }
}
