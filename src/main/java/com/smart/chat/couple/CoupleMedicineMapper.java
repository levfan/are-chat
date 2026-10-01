package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F275 服药提醒链数据访问。 */
@Mapper
public interface CoupleMedicineMapper extends BaseMapperCompat<CoupleMedicine> {

    /** 本人正在服用的药。 */
    default List<CoupleMedicine> findOngoingByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleMedicine>()
                .eq(CoupleMedicine::getSpaceId, spaceId)
                .eq(CoupleMedicine::getFromUser, fromUser)
                .eq(CoupleMedicine::getStatus, CoupleMedicine.STATUS_ONGOING));
    }

    /** 全部在服（双方共览）。 */
    default List<CoupleMedicine> findOngoing(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMedicine>()
                .eq(CoupleMedicine::getSpaceId, spaceId)
                .eq(CoupleMedicine::getStatus, CoupleMedicine.STATUS_ONGOING));
    }

    /** 同人同药一条。 */
    default CoupleMedicine find(String spaceId, String name, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleMedicine>()
                .eq(CoupleMedicine::getSpaceId, spaceId)
                .eq(CoupleMedicine::getName, name)
                .eq(CoupleMedicine::getFromUser, fromUser));
    }
}
