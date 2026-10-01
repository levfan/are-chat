package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F305 每日奥斯卡提名数据访问。 */
@Mapper
public interface CoupleActAwardMapper extends BaseMapperCompat<CoupleActAward> {

    default List<CoupleActAward> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleActAward>()
                .eq(CoupleActAward::getSpaceId, spaceId)
                .eq(CoupleActAward::getDay, day));
    }

    default CoupleActAward findByDayUser(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleActAward>()
                .eq(CoupleActAward::getSpaceId, spaceId)
                .eq(CoupleActAward::getDay, day)
                .eq(CoupleActAward::getFromUser, fromUser));
    }

    default List<CoupleActAward> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleActAward>()
                .eq(CoupleActAward::getSpaceId, spaceId)
                .orderByDesc(CoupleActAward::getDay));
    }
}
