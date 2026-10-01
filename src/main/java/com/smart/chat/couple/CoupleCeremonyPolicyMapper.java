package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F234 爱情保险柜保费记录数据访问。 */
@Mapper
public interface CoupleCeremonyPolicyMapper extends BaseMapperCompat<CoupleCeremonyPolicy> {

    /** 某人某月的保费。 */
    default CoupleCeremonyPolicy find(String spaceId, String month, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCeremonyPolicy>()
                .eq(CoupleCeremonyPolicy::getSpaceId, spaceId)
                .eq(CoupleCeremonyPolicy::getMonth, month)
                .eq(CoupleCeremonyPolicy::getFromUser, fromUser));
    }

    /** 空间内全部保费（按自然月聚合交齐数）。 */
    default List<CoupleCeremonyPolicy> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyPolicy>()
                .eq(CoupleCeremonyPolicy::getSpaceId, spaceId));
    }
}
