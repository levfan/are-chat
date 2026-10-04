package com.smart.chat.couple.domain.points;

import java.util.List;

/**
 * 积分台账的仓储端口：台账是只追加的流水，所以端口只有「追加」和「按空间读全量」两种说法。
 * <p>
 * 刻意不提供 update/delete：任何「改一笔已发生的分」都等于篡改账本，余额与心动值必须能从原始流水重算。
 */
public interface PointLedgerRepository {

    /** 记一笔（earn/spend 出来的流水） */
    void append(PointEntry entry);

    /** 该空间的全部流水（余额、心动值的 pointEarned 都从这里重算） */
    List<PointEntry> findBySpace(String spaceId);
}
