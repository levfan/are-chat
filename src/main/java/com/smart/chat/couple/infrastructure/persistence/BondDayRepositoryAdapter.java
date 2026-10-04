package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.streak.BondDay;
import com.smart.chat.couple.domain.streak.BondDayRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** {@link BondDayRepository} 的 MyBatis-Plus 适配器：只追加，没有回写路径。 */
@Component
public class BondDayRepositoryAdapter implements BondDayRepository {

    private final CoupleBondDayMapper bondDayMapper;

    public BondDayRepositoryAdapter(CoupleBondDayMapper bondDayMapper) {
        this.bondDayMapper = bondDayMapper;
    }

    @Override
    public List<BondDay> findBySpace(String spaceId) {
        return bondDayMapper.findBySpace(spaceId).stream().map(BondDayRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<BondDay> find(String spaceId, String day) {
        return Optional.ofNullable(bondDayMapper.find(spaceId, day)).map(BondDayRepositoryAdapter::toDomain);
    }

    @Override
    public long countMakeupBetween(String spaceId, String fromDay, String toDay) {
        return bondDayMapper.countMakeupBetween(spaceId, fromDay, toDay);
    }

    @Override
    public void append(BondDay bondDay) {
        CoupleBondDayPO po = new CoupleBondDayPO();
        po.setId(bondDay.id());
        po.setSpaceId(bondDay.spaceId());
        po.setDay(bondDay.day());
        po.setSource(bondDay.source());
        po.setOperatorUser(bondDay.operatorUser());
        po.setCreated(bondDay.created());
        bondDayMapper.insert(po);
    }

    private static BondDay toDomain(CoupleBondDayPO po) {
        return BondDay.restore(po.getId(), po.getSpaceId(), po.getDay(), po.getSource(), po.getOperatorUser(),
                po.getCreated());
    }
}
