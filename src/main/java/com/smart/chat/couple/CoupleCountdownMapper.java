package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCountdownMapper extends BaseMapperCompat<CoupleCountdown> {

    /** 某空间全部倒数日：期待中按日期升序在前，已实现归档在后。 */
    default List<CoupleCountdown> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCountdown>()
                .eq(CoupleCountdown::getSpaceId, spaceId)
                .orderByAsc(CoupleCountdown::getTargetDay));
    }
}
