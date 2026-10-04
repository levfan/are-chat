package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** {@link PointLedgerRepository} 的 MyBatis-Plus 适配器：台账只追加，所以这里没有回写路径。 */
@Component
public class PointLedgerRepositoryAdapter implements PointLedgerRepository {

    private final CouplePointLedgerMapper ledgerMapper;

    public PointLedgerRepositoryAdapter(CouplePointLedgerMapper ledgerMapper) {
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    public void append(PointEntry entry) {
        ledgerMapper.insert(toPo(entry));
    }

    @Override
    public List<PointEntry> findBySpace(String spaceId) {
        return ledgerMapper.findBySpace(spaceId).stream().map(PointLedgerRepositoryAdapter::toDomain).toList();
    }

    private static CouplePointLedgerPO toPo(PointEntry entry) {
        CouplePointLedgerPO po = new CouplePointLedgerPO();
        po.setId(entry.id());
        po.setSpaceId(entry.spaceId());
        po.setFromUser(entry.fromUser());
        po.setType(entry.type());
        po.setItem(entry.item());
        po.setPoints(entry.points());
        po.setCreated(entry.created());
        return po;
    }

    private static PointEntry toDomain(CouplePointLedgerPO po) {
        return PointEntry.restore(po.getId(), po.getSpaceId(), po.getFromUser(), po.getType(), po.getItem(),
                po.getPoints(), po.getCreated());
    }
}
