package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSecurityBankMapper extends BaseMapperCompat<CoupleSecurityBank> {

    /** 空间的安心话（新的在前）。 */
    default List<CoupleSecurityBank> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSecurityBank>()
                .eq(CoupleSecurityBank::getSpaceId, spaceId)
                .orderByDesc(CoupleSecurityBank::getCreated)
                .last("LIMIT 50"));
    }

    /** 已收下的安心话条数（账户余额）。 */
    default long countAccepted(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleSecurityBank>()
                .eq(CoupleSecurityBank::getSpaceId, spaceId)
                .eq(CoupleSecurityBank::getStatus, CoupleSecurityBank.STATUS_ACCEPTED));
    }
}
