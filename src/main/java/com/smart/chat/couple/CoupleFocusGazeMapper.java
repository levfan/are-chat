package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F364 对视十秒数据访问（uk(space_id,day) 每天一行，双方各点自己的列）。 */
@Mapper
public interface CoupleFocusGazeMapper extends BaseMapperCompat<CoupleFocusGaze> {

    /** 当天那行。 */
    default CoupleFocusGaze findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleFocusGaze>()
                .eq(CoupleFocusGaze::getSpaceId, spaceId)
                .eq(CoupleFocusGaze::getDay, day));
    }

    /** 某年全部（周报/年报计数用）。 */
    default List<CoupleFocusGaze> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleFocusGaze>()
                .eq(CoupleFocusGaze::getSpaceId, spaceId)
                .likeRight(CoupleFocusGaze::getDay, year + "-")
                .orderByAsc(CoupleFocusGaze::getDay));
    }
}
