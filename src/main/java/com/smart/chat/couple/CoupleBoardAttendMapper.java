package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F247 会议签到数据访问。 */
@Mapper
public interface CoupleBoardAttendMapper extends BaseMapperCompat<CoupleBoardAttend> {

    /** 某人某天的签到。 */
    default CoupleBoardAttend find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBoardAttend>()
                .eq(CoupleBoardAttend::getSpaceId, spaceId)
                .eq(CoupleBoardAttend::getDay, day)
                .eq(CoupleBoardAttend::getFromUser, fromUser));
    }

    /** 某天双方签到。 */
    default List<CoupleBoardAttend> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleBoardAttend>()
                .eq(CoupleBoardAttend::getSpaceId, spaceId)
                .eq(CoupleBoardAttend::getDay, day));
    }
}
