package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F226 周末慢生活数据访问。 */
@Mapper
public interface CoupleCozySlowMapper extends BaseMapperCompat<CoupleCozySlow> {

    /** 某人某周的慢生活小事。 */
    default CoupleCozySlow find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozySlow>()
                .eq(CoupleCozySlow::getSpaceId, spaceId)
                .eq(CoupleCozySlow::getWeek, week)
                .eq(CoupleCozySlow::getFromUser, fromUser));
    }

    default List<CoupleCozySlow> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleCozySlow>()
                .eq(CoupleCozySlow::getSpaceId, spaceId)
                .eq(CoupleCozySlow::getWeek, week));
    }
}
