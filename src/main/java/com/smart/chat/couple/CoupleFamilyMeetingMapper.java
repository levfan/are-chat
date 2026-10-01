package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFamilyMeetingMapper extends BaseMapperCompat<CoupleFamilyMeeting> {

    /** 全部会议（按周与时间倒序）。 */
    default List<CoupleFamilyMeeting> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFamilyMeeting>()
                .eq(CoupleFamilyMeeting::getSpaceId, spaceId)
                .orderByDesc(CoupleFamilyMeeting::getWeek)
                .orderByDesc(CoupleFamilyMeeting::getCreated));
    }
}
