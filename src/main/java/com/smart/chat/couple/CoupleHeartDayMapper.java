package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHeartDayMapper extends BaseMapperCompat<CoupleHeartDay> {

    /** 某人某天的心动标记。 */
    default CoupleHeartDay find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleHeartDay>()
                .eq(CoupleHeartDay::getSpaceId, spaceId)
                .eq(CoupleHeartDay::getDay, day)
                .eq(CoupleHeartDay::getFromUser, fromUser));
    }

    /** 全部标记（按天倒序）。 */
    default List<CoupleHeartDay> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHeartDay>()
                .eq(CoupleHeartDay::getSpaceId, spaceId)
                .orderByDesc(CoupleHeartDay::getDay));
    }
}
