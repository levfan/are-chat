package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F235 续约签字数据访问。 */
@Mapper
public interface CoupleCeremonyRenewMapper extends BaseMapperCompat<CoupleCeremonyRenew> {

    /** 某锚点日某人的签字。 */
    default CoupleCeremonyRenew find(String spaceId, String anchorDay, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCeremonyRenew>()
                .eq(CoupleCeremonyRenew::getSpaceId, spaceId)
                .eq(CoupleCeremonyRenew::getAnchorDay, anchorDay)
                .eq(CoupleCeremonyRenew::getFromUser, fromUser));
    }

    /** 某锚点日双方签字（攒长卷）。 */
    default List<CoupleCeremonyRenew> findByAnchor(String spaceId, String anchorDay) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyRenew>()
                .eq(CoupleCeremonyRenew::getSpaceId, spaceId)
                .eq(CoupleCeremonyRenew::getAnchorDay, anchorDay));
    }

    /** 空间内全部续约长卷。 */
    default List<CoupleCeremonyRenew> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyRenew>()
                .eq(CoupleCeremonyRenew::getSpaceId, spaceId));
    }
}
