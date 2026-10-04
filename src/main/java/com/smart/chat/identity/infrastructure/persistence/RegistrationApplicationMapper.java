package com.smart.chat.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RegistrationApplicationMapper extends BaseMapperCompat<RegistrationApplicationPO> {

    default Optional<RegistrationApplicationPO> findPendingByUsername(String username) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<RegistrationApplicationPO>()
                .eq(RegistrationApplicationPO::getUsername, username)
                .eq(RegistrationApplicationPO::getStatus, RegistrationApplicationPO.STATUS_PENDING)
                .last("LIMIT 1")));
    }

    default Optional<RegistrationApplicationPO> findPendingByPhone(String phone) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<RegistrationApplicationPO>()
                .eq(RegistrationApplicationPO::getPhone, phone)
                .eq(RegistrationApplicationPO::getStatus, RegistrationApplicationPO.STATUS_PENDING)
                .last("LIMIT 1")));
    }

    /** 用户名或手机号命中的最近一条申请（登录提示与注册进度查询用） */
    default Optional<RegistrationApplicationPO> findLatestByAccount(String account) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<RegistrationApplicationPO>()
                .and(w -> w.eq(RegistrationApplicationPO::getUsername, account)
                        .or().eq(RegistrationApplicationPO::getPhone, account))
                .orderByDesc(RegistrationApplicationPO::getCreated)
                .last("LIMIT 1")));
    }

    default List<RegistrationApplicationPO> findByStatus(String status) {
        LambdaQueryWrapper<RegistrationApplicationPO> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(RegistrationApplicationPO::getStatus, status);
        }
        wrapper.orderByAsc(RegistrationApplicationPO::getCreated);
        return selectList(wrapper);
    }

    default long countByStatus(String status) {
        return selectCount(new LambdaQueryWrapper<RegistrationApplicationPO>()
                .eq(RegistrationApplicationPO::getStatus, status));
    }
}
