package com.smart.chat.identity.domain.audit;

/**
 * 审计台账的仓储端口：只负责「记一笔」。
 * 流水表没有更新语义，所以这里<b>刻意没有 save/update</b>。
 */
public interface AdminAuditRepository {

    void append(AdminAudit audit);
}
