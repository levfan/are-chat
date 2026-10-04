package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleComfortMapper extends BaseMapperCompat<CoupleComfortPO> {

    /** 某人某天的求抱抱（每人每天一条）。 */
    default CoupleComfortPO find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleComfortPO>()
                .eq(CoupleComfortPO::getSpaceId, spaceId)
                .eq(CoupleComfortPO::getFromUser, fromUser)
                .eq(CoupleComfortPO::getDay, day));
    }

    /** 空间全部求抱抱记录（新→旧）。 */
    default List<CoupleComfortPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleComfortPO>()
                .eq(CoupleComfortPO::getSpaceId, spaceId)
                .orderByDesc(CoupleComfortPO::getCreated));
    }
}
