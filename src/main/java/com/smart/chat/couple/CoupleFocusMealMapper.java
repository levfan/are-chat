package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F363 饭桌不低头数据访问（uk(space_id,day) 每天一行，双方各点自己的列）。 */
@Mapper
public interface CoupleFocusMealMapper extends BaseMapperCompat<CoupleFocusMeal> {

    /** 当天那行。 */
    default CoupleFocusMeal findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFocusMeal>()
                .eq(CoupleFocusMeal::getSpaceId, spaceId)
                .eq(CoupleFocusMeal::getDay, day));
    }

    /** 空间全部（周报/年报计数用）。 */
    default List<CoupleFocusMeal> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFocusMeal>()
                .eq(CoupleFocusMeal::getSpaceId, spaceId)
                .orderByAsc(CoupleFocusMeal::getDay));
    }
}
