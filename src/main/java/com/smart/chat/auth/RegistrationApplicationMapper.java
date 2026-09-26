package com.smart.chat.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RegistrationApplicationMapper extends BaseMapperCompat<RegistrationApplication> {

    default Optional<RegistrationApplication> findPendingByUsername(String username) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<RegistrationApplication>()
                .eq(RegistrationApplication::getUsername, username)
                .eq(RegistrationApplication::getStatus, RegistrationApplication.STATUS_PENDING)
                .last("LIMIT 1")));
    }

    default Optional<RegistrationApplication> findPendingByPhone(String phone) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<RegistrationApplication>()
                .eq(RegistrationApplication::getPhone, phone)
                .eq(RegistrationApplication::getStatus, RegistrationApplication.STATUS_PENDING)
                .last("LIMIT 1")));
    }

    /** 用户名或手机号命中的最近一条申请（登录提示与注册进度查询用） */
    default Optional<RegistrationApplication> findLatestByAccount(String account) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<RegistrationApplication>()
                .and(w -> w.eq(RegistrationApplication::getUsername, account)
                        .or().eq(RegistrationApplication::getPhone, account))
                .orderByDesc(RegistrationApplication::getCreated)
                .last("LIMIT 1")));
    }

    default List<RegistrationApplication> findByStatus(String status) {
        LambdaQueryWrapper<RegistrationApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(RegistrationApplication::getStatus, status);
        }
        wrapper.orderByAsc(RegistrationApplication::getCreated);
        return selectList(wrapper);
    }

    default long countByStatus(String status) {
        return selectCount(new LambdaQueryWrapper<RegistrationApplication>()
                .eq(RegistrationApplication::getStatus, status));
    }
}
