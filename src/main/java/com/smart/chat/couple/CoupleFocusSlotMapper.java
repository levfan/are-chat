package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F361 专属时段数据访问（uk(space_id,week) 每周一格）。 */
@Mapper
public interface CoupleFocusSlotMapper extends BaseMapperCompat<CoupleFocusSlot> {

    /** 本周那格（提议/确认都写这一行）。 */
    default CoupleFocusSlot findByWeek(String spaceId, String week) {
        return selectOne(new LambdaQueryWrapper<CoupleFocusSlot>()
                .eq(CoupleFocusSlot::getSpaceId, spaceId)
                .eq(CoupleFocusSlot::getWeek, week));
    }

    /** 空间全部时段（新的在前，总览与周报用）。 */
    default List<CoupleFocusSlot> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFocusSlot>()
                .eq(CoupleFocusSlot::getSpaceId, spaceId)
                .orderByDesc(CoupleFocusSlot::getWeek));
    }
}
