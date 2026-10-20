package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleIntimateLogMapper extends BaseMapperCompat<CoupleIntimateLogPO> {

    /** 某空间的全部亲密记录：日期倒序，同一天按记录时刻倒序（yyyy-MM-dd 定长字符串，字典序即日期序）。 */
    default List<CoupleIntimateLogPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleIntimateLogPO>()
                .eq(CoupleIntimateLogPO::getSpaceId, spaceId)
                .orderByDesc(CoupleIntimateLogPO::getDay)
                .orderByDesc(CoupleIntimateLogPO::getCreated));
    }
}
