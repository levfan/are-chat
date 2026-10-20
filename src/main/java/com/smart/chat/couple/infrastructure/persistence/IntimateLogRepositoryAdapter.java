package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


/**
 * {@link IntimateLogRepository} 的 MyBatis-Plus 适配器：PO ↔ 领域对象的翻译只在这里。
 * 记录写下后不再修改，所以没有 update 分支。
 */
@Component
public class IntimateLogRepositoryAdapter implements IntimateLogRepository {

    private final CoupleIntimateLogMapper logMapper;

    public IntimateLogRepositoryAdapter(CoupleIntimateLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Override
    public List<IntimateLog> findBySpace(String spaceId) {
        return logMapper.findBySpace(spaceId).stream().map(IntimateLogRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<IntimateLog> findInRange(String spaceId, LocalDate startInclusive, LocalDate endExclusive) {
        return logMapper.findInRange(spaceId, startInclusive.toString(), endExclusive.toString()).stream()
                .map(IntimateLogRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<IntimateLog> findById(String recordId) {
        return Optional.ofNullable(logMapper.selectById(recordId)).map(IntimateLogRepositoryAdapter::toDomain);
    }

    @Override
    public void save(IntimateLog log) {
        CoupleIntimateLogPO row = new CoupleIntimateLogPO();
        row.setId(log.id());
        row.setSpaceId(log.spaceId());
        row.setDay(log.day());
        row.setKind(log.kind());
        row.setNote(log.note());
        row.setRecorderUser(log.recorderUser());
        row.setCreated(log.created());
        logMapper.insert(row);
    }

    @Override
    public void deleteById(String id) {
        logMapper.deleteById(id);
    }

    private static IntimateLog toDomain(CoupleIntimateLogPO po) {
        return IntimateLog.restore(po.getId(), po.getSpaceId(), po.getDay(), po.getKind(), po.getNote(),
                po.getRecorderUser(), po.getCreated());
    }
}
