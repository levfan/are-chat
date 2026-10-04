package com.smart.chat.identity.infrastructure.persistence;

import com.smart.chat.identity.domain.registration.RegistrationApplication;
import com.smart.chat.identity.domain.registration.RegistrationApplicationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link RegistrationApplicationRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 与账号适配器同一纪律：更新走 {@code selectById → 改聚合纳管的列 → updateById}，
 * 不用聚合重建整行（MyBatis-Plus 的 {@code updateById} 对 null 字段跳过，历史行里空的 nickname
 * 不会被这次改造弄出新的写入）。
 */
@Component
public class RegistrationApplicationRepositoryAdapter implements RegistrationApplicationRepository {

    private final RegistrationApplicationMapper applicationMapper;

    public RegistrationApplicationRepositoryAdapter(RegistrationApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    @Override
    public Optional<RegistrationApplication> findById(String id) {
        return Optional.ofNullable(applicationMapper.selectById(id)).map(RegistrationApplicationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<RegistrationApplication> findPendingByUsername(String username) {
        return applicationMapper.findPendingByUsername(username).map(RegistrationApplicationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<RegistrationApplication> findPendingByPhone(String phone) {
        return applicationMapper.findPendingByPhone(phone).map(RegistrationApplicationRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<RegistrationApplication> findLatestByAccount(String account) {
        return applicationMapper.findLatestByAccount(account).map(RegistrationApplicationRepositoryAdapter::toDomain);
    }

    @Override
    public List<RegistrationApplication> listByStatus(String status) {
        return applicationMapper.findByStatus(status).stream()
                .map(RegistrationApplicationRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countPending() {
        return applicationMapper.countByStatus(RegistrationApplication.STATUS_PENDING);
    }

    @Override
    public void save(RegistrationApplication application) {
        RegistrationApplicationPO existing = applicationMapper.selectById(application.id());
        if (existing == null) {
            applicationMapper.insert(toPo(application));
            return;
        }
        applyOwnedFields(existing, application);
        applicationMapper.updateById(existing);
    }

    private static RegistrationApplicationPO toPo(RegistrationApplication application) {
        RegistrationApplicationPO po = new RegistrationApplicationPO();
        po.setId(application.id());
        applyOwnedFields(po, application);
        return po;
    }

    /** 申请行的每一列都由聚合纳管（没有旁路写入方） */
    private static void applyOwnedFields(RegistrationApplicationPO po, RegistrationApplication application) {
        po.setPhone(application.phone());
        po.setUsername(application.username());
        po.setNickname(application.nickname());
        po.setPasswordHash(application.passwordHash());
        po.setStatus(application.status());
        po.setRejectReason(application.rejectReason());
        po.setCreated(application.created());
        po.setReviewedAt(application.reviewedAt());
        po.setReviewedBy(application.reviewedBy());
    }

    private static RegistrationApplication toDomain(RegistrationApplicationPO po) {
        return RegistrationApplication.restore(po.getId(), po.getPhone(), po.getUsername(), po.getNickname(),
                po.getPasswordHash(), po.getStatus(), po.getRejectReason(), po.getCreated(), po.getReviewedAt(),
                po.getReviewedBy());
    }
}
