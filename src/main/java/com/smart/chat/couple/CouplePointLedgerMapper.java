package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePointLedgerMapper extends BaseMapperCompat<CouplePointLedger> {

    /** 全部流水（按时间倒序）。 */
    default List<CouplePointLedger> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePointLedger>()
                .eq(CouplePointLedger::getSpaceId, spaceId)
                .orderByDesc(CouplePointLedger::getCreated));
    }
}
