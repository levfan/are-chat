package com.smart.chat.identity.infrastructure.persistence;

import com.smart.chat.identity.domain.audit.AdminAudit;
import com.smart.chat.identity.domain.audit.AdminAuditRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link AdminAuditRepository} 的 MyBatis-Plus 适配器：流水表只有追加与倒序读取，
 * 因此这里<b>没有更新分支</b>——想「改历史」在这个类里根本没有入口。
 */
@Component
public class AdminAuditRepositoryAdapter implements AdminAuditRepository {

    private final AdminAuditMapper auditMapper;

    public AdminAuditRepositoryAdapter(AdminAuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    @Override
    public void append(AdminAudit audit) {
        AdminAuditPO po = new AdminAuditPO();
        po.setId(audit.id());
        po.setActor(audit.actor());
        po.setAction(audit.action());
        po.setTarget(audit.target());
        po.setDetail(audit.detail());
        po.setCreated(audit.created());
        auditMapper.insert(po);
    }

    @Override
    public List<AdminAudit> listLatest(int limit) {
        return auditMapper.findLatest(limit).stream()
                .map(po -> AdminAudit.restore(po.getId(), po.getActor(), po.getAction(), po.getTarget(),
                        po.getDetail(), po.getCreated()))
                .toList();
    }
}
