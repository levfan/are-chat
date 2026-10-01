package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F239 当日体感留言数据访问。 */
@Mapper
public interface CoupleCeremonyRecapMapper extends BaseMapperCompat<CoupleCeremonyRecap> {

    /** 某天某人的留言。 */
    default CoupleCeremonyRecap find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCeremonyRecap>()
                .eq(CoupleCeremonyRecap::getSpaceId, spaceId)
                .eq(CoupleCeremonyRecap::getDay, day)
                .eq(CoupleCeremonyRecap::getFromUser, fromUser));
    }

    /** 空间内全部留言（史册聚合用）。 */
    default List<CoupleCeremonyRecap> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyRecap>()
                .eq(CoupleCeremonyRecap::getSpaceId, spaceId));
    }
}
