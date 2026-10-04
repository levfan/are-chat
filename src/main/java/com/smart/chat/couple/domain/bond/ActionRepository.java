package com.smart.chat.couple.domain.bond;

import java.util.List;

/**
 * 贴贴流水的仓储端口：只追加的流水只需要「记一次」和「按口径数一数」两种说法。
 * <p>
 * 刻意不提供 update/delete：贴贴发出去就不能改不能删，今日计数与里程碑都必须能从原始流水算出来。
 * 三个统计读法直接返回标量——读投影不是聚合，不为它造类型（见 {@code docs/ddd/05-tactical-playbook.md} 2.3）。
 */
public interface ActionRepository {

    /** 记一次贴贴（新的一行，id 与时刻已由聚合盖好）。 */
    void append(BondAction action);

    /** 该空间全部动作（新→旧，动作流与今日计数都用它）。 */
    List<BondAction> listBySpace(String spaceId);

    /** 某类动作在这个空间累计多少次（里程碑踩线用）。 */
    long countByKind(String spaceId, String kind);

    /** 某类动作里某人发过多少次。 */
    long countSentBy(String spaceId, String kind, String username);

    /** 某类动作最近一次的时刻，没发过返回 null。 */
    Long lastSentAt(String spaceId, String kind);
}
