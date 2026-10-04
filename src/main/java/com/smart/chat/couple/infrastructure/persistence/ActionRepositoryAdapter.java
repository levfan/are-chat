package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.bond.BondAction;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link ActionRepository} 的 MyBatis-Plus 适配器：PO ↔ 薄实体的翻译只发生在这里。
 * <p>
 * 排序与统计一律复用 {@link CoupleActionMapper} 已有的 default 方法（Mapper 不感知领域类型），
 * 「新→旧」「按类别计数」「最近一次」的口径因此与改造前逐字相同。
 * <p>
 * 流水只追加，所以这里<b>没有任何回写</b>：既不 update 也不 delete。
 */
@Component
public class ActionRepositoryAdapter implements ActionRepository {

    private final CoupleActionMapper actionMapper;

    public ActionRepositoryAdapter(CoupleActionMapper actionMapper) {
        this.actionMapper = actionMapper;
    }

    @Override
    public void append(BondAction action) {
        actionMapper.insert(toPo(action));
    }

    @Override
    public List<BondAction> listBySpace(String spaceId) {
        return actionMapper.findBySpace(spaceId).stream().map(ActionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countByKind(String spaceId, String kind) {
        return actionMapper.countByKind(spaceId, kind);
    }

    @Override
    public long countSentBy(String spaceId, String kind, String username) {
        return actionMapper.countByKindAndUser(spaceId, kind, username);
    }

    @Override
    public Long lastSentAt(String spaceId, String kind) {
        return actionMapper.lastCreatedAt(spaceId, kind);
    }

    @Override
    public List<BondAction> listSentSince(String spaceId, long fromMillis) {
        return actionMapper.findSince(spaceId, fromMillis).stream().map(ActionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countAll() {
        return actionMapper.selectCount(null);
    }

    private static CoupleActionPO toPo(BondAction action) {
        CoupleActionPO po = new CoupleActionPO();
        po.setId(action.id());
        po.setSpaceId(action.spaceId());
        po.setUsername(action.username());
        po.setKind(action.kind());
        po.setCreated(action.created());
        return po;
    }

    private static BondAction toDomain(CoupleActionPO po) {
        return BondAction.restore(po.getId(), po.getSpaceId(), po.getUsername(), po.getKind(), po.getCreated());
    }
}
