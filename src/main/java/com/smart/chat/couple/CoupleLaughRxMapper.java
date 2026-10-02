package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F396 大笑处方数据访问（uk(space_id,from_user,day) 每人每天一张）。 */
@Mapper
public interface CoupleLaughRxMapper extends BaseMapperCompat<CoupleLaughRx> {

    /** 空间全部处方（日子降序，最近开的在前）。 */
    default List<CoupleLaughRx> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLaughRx>()
                .eq(CoupleLaughRx::getSpaceId, spaceId)
                .orderByDesc(CoupleLaughRx::getDay));
    }

    /** 按 uk 定位那一张（写前查重、服用回执回填用）。 */
    default CoupleLaughRx find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleLaughRx>()
                .eq(CoupleLaughRx::getSpaceId, spaceId)
                .eq(CoupleLaughRx::getFromUser, fromUser)
                .eq(CoupleLaughRx::getDay, day));
    }
}
