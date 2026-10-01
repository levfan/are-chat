package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F302 师徒日数据访问。 */
@Mapper
public interface CoupleMasterDayMapper extends BaseMapperCompat<CoupleMasterDay> {

    default CoupleMasterDay findByWeek(String spaceId, String week) {
        return selectOne(new LambdaQueryWrapper<CoupleMasterDay>()
                .eq(CoupleMasterDay::getSpaceId, spaceId)
                .eq(CoupleMasterDay::getWeek, week));
    }

    default List<CoupleMasterDay> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMasterDay>()
                .eq(CoupleMasterDay::getSpaceId, spaceId)
                .orderByDesc(CoupleMasterDay::getWeek));
    }
}
