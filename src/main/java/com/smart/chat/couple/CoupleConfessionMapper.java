package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleConfessionMapper extends BaseMapperCompat<CoupleConfession> {

    /** 空间的告白存档（按告白日期升序）。 */
    default List<CoupleConfession> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleConfession>()
                .eq(CoupleConfession::getSpaceId, spaceId)
                .orderByAsc(CoupleConfession::getConfessDay));
    }

    /** 每年的今天（MM-dd 匹配任意年份）需要重播的告白，未在本年重播过的留给 Job 过滤。 */
    default List<CoupleConfession> findByMonthDay(String monthDay) {
        return selectList(new LambdaQueryWrapper<CoupleConfession>()
                .like(CoupleConfession::getConfessDay, monthDay));
    }
}
