package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleComfortMapper extends BaseMapperCompat<CoupleComfort> {

    /** 某人某天的求抱抱（每人每天一条）。 */
    default CoupleComfort find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleComfort>()
                .eq(CoupleComfort::getSpaceId, spaceId)
                .eq(CoupleComfort::getFromUser, fromUser)
                .eq(CoupleComfort::getDay, day));
    }

    /** 空间全部求抱抱记录（新→旧）。 */
    default List<CoupleComfort> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleComfort>()
                .eq(CoupleComfort::getSpaceId, spaceId)
                .orderByDesc(CoupleComfort::getCreated));
    }
}
