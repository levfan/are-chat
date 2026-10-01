package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleBlindPickMapper extends BaseMapperCompat<CoupleBlindPick> {

    /** 某周的盲选提交。 */
    default List<CoupleBlindPick> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleBlindPick>()
                .eq(CoupleBlindPick::getSpaceId, spaceId)
                .eq(CoupleBlindPick::getWeek, week));
    }

    /** 空间历史盲选（新的在前）。 */
    default List<CoupleBlindPick> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBlindPick>()
                .eq(CoupleBlindPick::getSpaceId, spaceId)
                .orderByDesc(CoupleBlindPick::getCreated)
                .last("LIMIT 20"));
    }
}
