package com.smart.chat.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AppUserMapper extends BaseMapperCompat<AppUser> {

    default Optional<AppUser> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username)
                .last("LIMIT 1")));
    }

    default Optional<AppUser> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, phone)
                .last("LIMIT 1")));
    }

    /** 用户名或手机号精确命中（登录用，account 已由上层规范化为小写/去空格） */
    default Optional<AppUser> findByAccount(String account) {
        return findByUsername(account).or(() -> findByPhone(account));
    }

    /** 联想候选：用户名或手机号包含关键字，按用户名排序 */
    default List<AppUser> search(String keyword, int limit) {
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getStatus, AppUser.STATUS_ACTIVE);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AppUser::getUsername, keyword).or().like(AppUser::getPhone, keyword));
        }
        wrapper.orderByAsc(AppUser::getUsername).last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return selectList(wrapper);
    }

    default long countUsers() {
        return selectCount(new LambdaQueryWrapper<>());
    }
}
