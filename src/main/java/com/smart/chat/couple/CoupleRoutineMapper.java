package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleRoutineMapper extends BaseMapperCompat<CoupleRoutine> {

    /** 空间的双方作息。 */
    default List<CoupleRoutine> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRoutine>()
                .eq(CoupleRoutine::getSpaceId, spaceId)
                .orderByAsc(CoupleRoutine::getOwnerUser));
    }

    /** 某人的作息。 */
    default CoupleRoutine findByOwner(String spaceId, String ownerUser) {
        return selectOne(new LambdaQueryWrapper<CoupleRoutine>()
                .eq(CoupleRoutine::getSpaceId, spaceId)
                .eq(CoupleRoutine::getOwnerUser, ownerUser));
    }
}
