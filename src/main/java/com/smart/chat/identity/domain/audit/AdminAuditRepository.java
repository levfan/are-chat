package com.smart.chat.identity.domain.audit;

import java.util.List;

/**
 * 审计台账的仓储端口：只有「记一笔」和「按时间倒序取最近若干条」两种读法。
 * 流水表没有更新语义，所以这里<b>刻意没有 save/update</b>。
 */
public interface AdminAuditRepository {

    void append(AdminAudit audit);

    /** 最近 limit 条（按操作时间倒序，limit 由上层夹在 1~200） */
    List<AdminAudit> listLatest(int limit);
}
