package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F254 节日家档数据访问。 */
@Mapper
public interface CoupleFestivalPlanMapper extends BaseMapperCompat<CoupleFestivalPlan> {

    /** 某人某年某节日的一案。 */
    default CoupleFestivalPlan find(String spaceId, String festival, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleFestivalPlan>()
                .eq(CoupleFestivalPlan::getSpaceId, spaceId)
                .eq(CoupleFestivalPlan::getFestival, festival)
                .eq(CoupleFestivalPlan::getYear, year)
                .eq(CoupleFestivalPlan::getFromUser, fromUser));
    }

    /** 某年全部方案。 */
    default List<CoupleFestivalPlan> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleFestivalPlan>()
                .eq(CoupleFestivalPlan::getSpaceId, spaceId)
                .eq(CoupleFestivalPlan::getYear, year));
    }
}
