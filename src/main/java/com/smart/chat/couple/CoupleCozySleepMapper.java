package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F221 睡眠报告单数据访问。 */
@Mapper
public interface CoupleCozySleepMapper extends BaseMapperCompat<CoupleCozySleep> {

    /** 某人某天的一条。 */
    default CoupleCozySleep find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozySleep>()
                .eq(CoupleCozySleep::getSpaceId, spaceId)
                .eq(CoupleCozySleep::getDay, day)
                .eq(CoupleCozySleep::getFromUser, fromUser));
    }

    /** 一段时间的睡眠单（月度小结）。 */
    default List<CoupleCozySleep> findRange(String spaceId, String fromDay, String toDay) {
        return selectList(new LambdaQueryWrapper<CoupleCozySleep>()
                .eq(CoupleCozySleep::getSpaceId, spaceId)
                .between(CoupleCozySleep::getDay, fromDay, toDay));
    }
}
