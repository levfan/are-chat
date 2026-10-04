package com.smart.chat.identity.domain.account;

import java.util.List;
import java.util.Optional;

/**
 * 账号聚合的仓储端口：领域只说「按用户名/手机号取账号」「把它存回去」，
 * 走哪个 Mapper、哪一列归谁写，都是 infrastructure 的事（手法见 {@code docs/ddd/05-tactical-playbook.md} 第 2.3/2.4 节）。
 * <p>
 * 参数与返回值只允许领域类型和 JDK 类型——{@code AppUserPO} 与 {@code LambdaQueryWrapper} 一律不出现在这里。
 */
public interface AccountRepository {

    /** 按规范化后的用户名取账号 */
    Optional<Account> findByUsername(String username);

    Optional<Account> findByPhone(String phone);

    /** 用户名或手机号精确命中（登录用；account 已由 {@code AccountRules} 规范化） */
    Optional<Account> findByAccount(String account);

    /** 全部管理员（审批待办的收件人） */
    List<Account> listAdmins();

    long countAdmins();

    /** 通讯录联想候选：只含 ACTIVE，按用户名排序 */
    List<Account> listSearchCandidates(String keyword, int limit);

    /** 管理后台视角：全部状态都要能搜到 */
    List<Account> listForAdmin(String keyword, int limit);

    /** 占用了这些手机号且仍处于 ACTIVE 的账号（启动引导用来禁用旧版演示账号） */
    List<Account> listActiveAmongPhones(List<String> phones);

    /** 新账号落库；已有账号只回写聚合纳管的那几列（其余列保持原值） */
    void save(Account account);
}
