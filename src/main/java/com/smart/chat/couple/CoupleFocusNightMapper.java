package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F360 专注打卡数据访问（uk(space_id,day) 每天最多一行，双人各写自己的列）。 */
@Mapper
public interface CoupleFocusNightMapper extends BaseMapperCompat<CoupleFocusNight> {

    /** 当夜那行（双方共写一行）。 */
    default CoupleFocusNight findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFocusNight>()
                .eq(CoupleFocusNight::getSpaceId, spaceId)
                .eq(CoupleFocusNight::getDay, day));
    }

    /** [fromDay,toDay] 闭区间（周报/年报/最专注一天用）。 */
    default List<CoupleFocusNight> findByDayRange(String spaceId, String fromDay, String toDay) {
        return selectList(new LambdaQueryWrapper<CoupleFocusNight>()
                .eq(CoupleFocusNight::getSpaceId, spaceId)
                .ge(CoupleFocusNight::getDay, fromDay)
                .le(CoupleFocusNight::getDay, toDay)
                .orderByAsc(CoupleFocusNight::getDay));
    }

    /** 某年全部（年报用，yyyy 前缀）。 */
    default List<CoupleFocusNight> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleFocusNight>()
                .eq(CoupleFocusNight::getSpaceId, spaceId)
                .likeRight(CoupleFocusNight::getDay, year + "-")
                .orderByAsc(CoupleFocusNight::getDay));
    }
}
