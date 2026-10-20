package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link PeriodRepository} 的 MyBatis-Plus 适配器：PO ↔ 领域对象的翻译只在这里。
 * 更新只回写结束日与修改时刻——空间、开始日、记录人一旦写下就不再改。
 */
@Component
public class PeriodRepositoryAdapter implements PeriodRepository {

    private final CouplePeriodMapper periodMapper;

    public PeriodRepositoryAdapter(CouplePeriodMapper periodMapper) {
        this.periodMapper = periodMapper;
    }

    @Override
    public List<PeriodRecord> findBySpace(String spaceId) {
        return periodMapper.findBySpace(spaceId).stream().map(PeriodRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<PeriodRecord> findById(String recordId) {
        return Optional.ofNullable(periodMapper.selectById(recordId)).map(PeriodRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByStart(String spaceId, String startDay) {
        return periodMapper.existsByStart(spaceId, startDay);
    }

    @Override
    public void save(PeriodRecord record) {
        CouplePeriodPO existing = periodMapper.selectById(record.id());
        if (existing == null) {
            CouplePeriodPO row = new CouplePeriodPO();
            row.setId(record.id());
            row.setSpaceId(record.spaceId());
            row.setStartDay(record.startDay());
            row.setEndDay(record.endDay());
            row.setRecorderUser(record.recorderUser());
            row.setCreated(record.created());
            periodMapper.insert(row);
            return;
        }
        existing.setEndDay(record.endDay());
        existing.setUpdatedAt(System.currentTimeMillis());
        periodMapper.updateById(existing);
    }

    @Override
    public void deleteById(String id) {
        periodMapper.deleteById(id);
    }

    private static PeriodRecord toDomain(CouplePeriodPO po) {
        return PeriodRecord.restore(po.getId(), po.getSpaceId(), po.getStartDay(), po.getEndDay(),
                po.getRecorderUser(), po.getCreated());
    }
}
