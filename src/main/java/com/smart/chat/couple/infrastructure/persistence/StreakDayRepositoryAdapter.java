package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.streak.StreakDay;
import com.smart.chat.couple.domain.streak.StreakDayRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** {@link StreakDayRepository} 的 MyBatis-Plus 适配器：只追加，没有回写路径。 */
@Component
public class StreakDayRepositoryAdapter implements StreakDayRepository {

    private final CoupleStreakDayMapper streakDayMapper;

    public StreakDayRepositoryAdapter(CoupleStreakDayMapper streakDayMapper) {
        this.streakDayMapper = streakDayMapper;
    }

    @Override
    public List<StreakDay> findBySpace(String spaceId) {
        return streakDayMapper.findBySpace(spaceId).stream().map(StreakDayRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<StreakDay> find(String spaceId, String day) {
        return Optional.ofNullable(streakDayMapper.find(spaceId, day)).map(StreakDayRepositoryAdapter::toDomain);
    }

    @Override
    public long countMakeupBetween(String spaceId, String fromDay, String toDay) {
        return streakDayMapper.countMakeupBetween(spaceId, fromDay, toDay);
    }

    @Override
    public long countAll() {
        return streakDayMapper.countAll();
    }

    @Override
    public void append(StreakDay streakDay) {
        CoupleStreakDayPO po = new CoupleStreakDayPO();
        po.setId(streakDay.id());
        po.setSpaceId(streakDay.spaceId());
        po.setDay(streakDay.day());
        po.setSource(streakDay.source());
        po.setOperatorUser(streakDay.operatorUser());
        po.setCreated(streakDay.created());
        streakDayMapper.insert(po);
    }

    private static StreakDay toDomain(CoupleStreakDayPO po) {
        return StreakDay.restore(po.getId(), po.getSpaceId(), po.getDay(), po.getSource(), po.getOperatorUser(),
                po.getCreated());
    }
}
