package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDineWeekplanMapper extends BaseMapperCompat<CoupleDineWeekplan> {

    /** 某周某天的菜单格。 */
    default CoupleDineWeekplan find(String spaceId, String week, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleDineWeekplan>()
                .eq(CoupleDineWeekplan::getSpaceId, spaceId)
                .eq(CoupleDineWeekplan::getWeek, week)
                .eq(CoupleDineWeekplan::getDay, day));
    }

    /** 某周整排菜单。 */
    default List<CoupleDineWeekplan> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleDineWeekplan>()
                .eq(CoupleDineWeekplan::getSpaceId, spaceId)
                .eq(CoupleDineWeekplan::getWeek, week)
                .orderByAsc(CoupleDineWeekplan::getDay));
    }

    /** 某年排下的所有正餐格（年度干饭账）。 */
    default List<CoupleDineWeekplan> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleDineWeekplan>()
                .eq(CoupleDineWeekplan::getSpaceId, spaceId)
                .likeRight(CoupleDineWeekplan::getDay, year + "-"));
    }
}
