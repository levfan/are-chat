package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleWeeklyStarMapper extends BaseMapperCompat<CoupleWeeklyStar> {

    /** 某人某周的提名。 */
    default CoupleWeeklyStar find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleWeeklyStar>()
                .eq(CoupleWeeklyStar::getSpaceId, spaceId)
                .eq(CoupleWeeklyStar::getWeek, week)
                .eq(CoupleWeeklyStar::getFromUser, fromUser));
    }

    /** 全部高光提名（按创建倒序）。 */
    default List<CoupleWeeklyStar> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWeeklyStar>()
                .eq(CoupleWeeklyStar::getSpaceId, spaceId)
                .orderByDesc(CoupleWeeklyStar::getCreated));
    }
}
