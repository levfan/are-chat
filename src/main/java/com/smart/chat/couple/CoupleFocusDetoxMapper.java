package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F368 数字排毒半天数据访问（uk(space_id,day) 一天一格，双方各报自己的列）。 */
@Mapper
public interface CoupleFocusDetoxMapper extends BaseMapperCompat<CoupleFocusDetox> {

    /** 当天那格（发起与应战都写这一行）。 */
    default CoupleFocusDetox findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFocusDetox>()
                .eq(CoupleFocusDetox::getSpaceId, spaceId)
                .eq(CoupleFocusDetox::getDay, day));
    }

    /** 空间全部（周报/年报计数用）。 */
    default List<CoupleFocusDetox> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFocusDetox>()
                .eq(CoupleFocusDetox::getSpaceId, spaceId)
                .orderByAsc(CoupleFocusDetox::getDay));
    }
}
