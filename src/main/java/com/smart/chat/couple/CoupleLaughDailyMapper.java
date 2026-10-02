package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F391 每日一逗数据访问（uk(space_id,day) 一天一格，双人共写这一行）。 */
@Mapper
public interface CoupleLaughDailyMapper extends BaseMapperCompat<CoupleLaughDaily> {

    /** 空间全部日逗（日子降序，回看谁负责哪天）。 */
    default List<CoupleLaughDaily> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughDaily>()
                .eq(CoupleLaughDaily::getSpaceId, spaceId)
                .orderByDesc(CoupleLaughDaily::getDay));
    }

    /** 那一天那一格（双方共写一行）。 */
    default CoupleLaughDaily findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughDaily>()
                .eq(CoupleLaughDaily::getSpaceId, spaceId)
                .eq(CoupleLaughDaily::getDay, day));
    }
}
