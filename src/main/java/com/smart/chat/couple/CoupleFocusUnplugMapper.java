package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F365 不插电半小时数据访问（uk(space_id,day) 每天一行，双点日才算连击）。 */
@Mapper
public interface CoupleFocusUnplugMapper extends BaseMapperCompat<CoupleFocusUnplug> {

    /** 当天那行。 */
    default CoupleFocusUnplug findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFocusUnplug>()
                .eq(CoupleFocusUnplug::getSpaceId, spaceId)
                .eq(CoupleFocusUnplug::getDay, day));
    }

    /** 某年全部（周连击与年报计数用）。 */
    default List<CoupleFocusUnplug> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleFocusUnplug>()
                .eq(CoupleFocusUnplug::getSpaceId, spaceId)
                .likeRight(CoupleFocusUnplug::getDay, year + "-")
                .orderByAsc(CoupleFocusUnplug::getDay));
    }
}
