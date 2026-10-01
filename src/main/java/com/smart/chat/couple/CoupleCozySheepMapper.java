package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F222 数羊房数据访问。 */
@Mapper
public interface CoupleCozySheepMapper extends BaseMapperCompat<CoupleCozySheep> {

    /** 某人某天的一条。 */
    default CoupleCozySheep find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozySheep>()
                .eq(CoupleCozySheep::getSpaceId, spaceId)
                .eq(CoupleCozySheep::getDay, day)
                .eq(CoupleCozySheep::getFromUser, fromUser));
    }

    default List<CoupleCozySheep> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleCozySheep>()
                .eq(CoupleCozySheep::getSpaceId, spaceId)
                .eq(CoupleCozySheep::getDay, day));
    }

    /** 一段时间的数羊记录（月度小结）。 */
    default List<CoupleCozySheep> findRange(String spaceId, String fromDay, String toDay) {
        return selectList(new LambdaQueryWrapper<CoupleCozySheep>()
                .eq(CoupleCozySheep::getSpaceId, spaceId)
                .between(CoupleCozySheep::getDay, fromDay, toDay));
    }
}
