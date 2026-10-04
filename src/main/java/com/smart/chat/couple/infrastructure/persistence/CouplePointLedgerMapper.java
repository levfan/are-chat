package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePointLedgerMapper extends BaseMapperCompat<CouplePointLedgerPO> {

    /** 全部流水（按时间倒序）。 */
    default List<CouplePointLedgerPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePointLedgerPO>()
                .eq(CouplePointLedgerPO::getSpaceId, spaceId)
                .orderByDesc(CouplePointLedgerPO::getCreated));
    }
}
