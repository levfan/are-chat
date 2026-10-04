package com.smart.chat.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AppUserMapper extends BaseMapperCompat<AppUserPO> {

    default Optional<AppUserPO> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<AppUserPO>()
                .eq(AppUserPO::getUsername, username)
                .last("LIMIT 1")));
    }

    default Optional<AppUserPO> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<AppUserPO>()
                .eq(AppUserPO::getPhone, phone)
                .last("LIMIT 1")));
    }

    /** 用户名或手机号精确命中（登录用，account 已由领域侧 AccountRules 规范化为小写/去空格） */
    default Optional<AppUserPO> findByAccount(String account) {
        return findByUsername(account).or(() -> findByPhone(account));
    }

    /** 联想候选：用户名或手机号包含关键字，按用户名排序 */
    default List<AppUserPO> search(String keyword, int limit) {
        LambdaQueryWrapper<AppUserPO> wrapper = new LambdaQueryWrapper<AppUserPO>()
                .eq(AppUserPO::getStatus, AppUserPO.STATUS_ACTIVE);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AppUserPO::getUsername, keyword).or().like(AppUserPO::getPhone, keyword));
        }
        wrapper.orderByAsc(AppUserPO::getUsername).last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return selectList(wrapper);
    }

    default long countUsers() {
        return selectCount(new LambdaQueryWrapper<>());
    }

    /** 79 用户管理：按用户名/手机号模糊搜索全部状态用户（管理员视角） */
    default List<AppUserPO> searchAll(String keyword, int limit) {
        LambdaQueryWrapper<AppUserPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AppUserPO::getUsername, kw).or().like(AppUserPO::getPhone, kw));
        }
        wrapper.orderByAsc(AppUserPO::getUsername).last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return selectList(wrapper);
    }

    default List<AppUserPO> findAdmins() {
        return selectList(new LambdaQueryWrapper<AppUserPO>().eq(AppUserPO::getRole, AppUserPO.ROLE_ADMIN));
    }

    default long countAdmins() {
        return selectCount(new LambdaQueryWrapper<AppUserPO>().eq(AppUserPO::getRole, AppUserPO.ROLE_ADMIN));
    }

    /** 启动引导禁用旧版演示账号用：命中这些手机号且仍 ACTIVE 的行 */
    default List<AppUserPO> findActiveAmongPhones(Collection<String> phones) {
        if (phones == null || phones.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<AppUserPO>()
                .in(AppUserPO::getPhone, phones)
                .eq(AppUserPO::getStatus, AppUserPO.STATUS_ACTIVE));
    }
}
