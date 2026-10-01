package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleReadMinuteMapper extends BaseMapperCompat<CoupleReadMinute> {

    /** 某人某天的共读感想。 */
    default CoupleReadMinute find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleReadMinute>()
                .eq(CoupleReadMinute::getSpaceId, spaceId)
                .eq(CoupleReadMinute::getDay, day)
                .eq(CoupleReadMinute::getFromUser, fromUser));
    }

    /** 全部共读感想（按创建倒序）。 */
    default List<CoupleReadMinute> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleReadMinute>()
                .eq(CoupleReadMinute::getSpaceId, spaceId)
                .orderByDesc(CoupleReadMinute::getCreated));
    }
}
