package com.smart.chat.identity.domain.registration;

import java.util.List;
import java.util.Optional;

/**
 * 注册申请的仓储端口。方法名一律用领域语言（{@code findPendingBy…}/{@code countPending}），
 * 不出现 select/update/Wrapper；参数与返回值不含 PO。
 */
public interface RegistrationApplicationRepository {

    Optional<RegistrationApplication> findById(String id);

    /** 同名用户名的在途申请（用来挡住重复提交） */
    Optional<RegistrationApplication> findPendingByUsername(String username);

    Optional<RegistrationApplication> findPendingByPhone(String phone);

    /** 用户名或手机号命中的最近一条申请（登录提示与注册进度查询用） */
    Optional<RegistrationApplication> findLatestByAccount(String account);

    /** 审批队列：status 为空则返回全部，按申请时间升序 */
    List<RegistrationApplication> listByStatus(String status);

    /** 待办角标的数字 */
    long countPending();

    /** 新申请落库；已处理的申请只回写聚合纳管的列 */
    void save(RegistrationApplication application);
}
