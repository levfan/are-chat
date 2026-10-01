package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F262 误会倒带数据访问。 */
@Mapper
public interface CoupleMisrewindMapper extends BaseMapperCompat<CoupleMisrewind> {

    /** 某人某日某主题的一份。 */
    default CoupleMisrewind find(String spaceId, String day, String topic, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleMisrewind>()
                .eq(CoupleMisrewind::getSpaceId, spaceId)
                .eq(CoupleMisrewind::getDay, day)
                .eq(CoupleMisrewind::getTopic, topic)
                .eq(CoupleMisrewind::getFromUser, fromUser));
    }

    /** 某天全部倒带卡。 */
    default List<CoupleMisrewind> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleMisrewind>()
                .eq(CoupleMisrewind::getSpaceId, spaceId)
                .eq(CoupleMisrewind::getDay, day));
    }

    /** 近 30 天（列表回放）。 */
    default List<CoupleMisrewind> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleMisrewind>()
                .eq(CoupleMisrewind::getSpaceId, spaceId)
                .ge(CoupleMisrewind::getDay, fromDay)
                .orderByDesc(CoupleMisrewind::getDay));
    }
}
