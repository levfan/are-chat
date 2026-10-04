package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleUserPinMapper extends BaseMapperCompat<CoupleUserPinPO> {

    /** 某人的收藏行。 */
    default CoupleUserPinPO find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleUserPinPO>()
                .eq(CoupleUserPinPO::getSpaceId, spaceId)
                .eq(CoupleUserPinPO::getFromUser, fromUser));
    }

    /** 空间内全部收藏行。 */
    default List<CoupleUserPinPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleUserPinPO>()
                .eq(CoupleUserPinPO::getSpaceId, spaceId));
    }
}
