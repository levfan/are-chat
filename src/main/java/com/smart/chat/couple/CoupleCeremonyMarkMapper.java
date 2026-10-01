package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F233 庆祝打卡数据访问。 */
@Mapper
public interface CoupleCeremonyMarkMapper extends BaseMapperCompat<CoupleCeremonyMark> {

    /** 某过法卡某天的勾。 */
    default CoupleCeremonyMark find(String ritualId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleCeremonyMark>()
                .eq(CoupleCeremonyMark::getRitualId, ritualId)
                .eq(CoupleCeremonyMark::getDay, day));
    }

    /** 空间内全部打卡记录（史册/加冕聚合用）。 */
    default List<CoupleCeremonyMark> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyMark>()
                .eq(CoupleCeremonyMark::getSpaceId, spaceId));
    }

    /** 删除某小日子名下的全部打卡（先按 ritualIds 过滤）。 */
    default void deleteByRitualIds(List<String> ritualIds) {
        if (ritualIds == null || ritualIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapper<CoupleCeremonyMark>()
                .in(CoupleCeremonyMark::getRitualId, ritualIds));
    }
}
