package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleAnniversaryMapper extends BaseMapperCompat<CoupleAnniversaryPO> {

    default List<CoupleAnniversaryPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAnniversaryPO>()
                .eq(CoupleAnniversaryPO::getSpaceId, spaceId)
                .orderByAsc(CoupleAnniversaryPO::getEventDate)
                .orderByAsc(CoupleAnniversaryPO::getCreated));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleAnniversaryPO>().eq(CoupleAnniversaryPO::getSpaceId, spaceId));
    }
}
