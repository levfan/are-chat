package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.safeword.Safeword;
import com.smart.chat.couple.domain.safeword.SafewordRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link SafewordRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleCatchSafewordMapper} 已有的 default 方法，排序口径与改造前逐字相同。
 * {@link #save} 沿用 {@code CoupleSpaceRepositoryAdapter} 的纪律——upsert：没约过整行新建，
 * 约过则先按 (space, 人) 取回原行，只改写词与说明与更新时间，其余列（id/spaceId/fromUser/created）原样留着。
 */
@Component
public class SafewordRepositoryAdapter implements SafewordRepository {

    private final CoupleCatchSafewordMapper safewordMapper;

    public SafewordRepositoryAdapter(CoupleCatchSafewordMapper safewordMapper) {
        this.safewordMapper = safewordMapper;
    }

    @Override
    public Optional<Safeword> findBySpaceAndUser(String spaceId, String fromUser) {
        return Optional.ofNullable(safewordMapper.find(spaceId, fromUser)).map(SafewordRepositoryAdapter::toDomain);
    }

    @Override
    public List<Safeword> listBySpace(String spaceId) {
        return safewordMapper.findBySpace(spaceId).stream().map(SafewordRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(String spaceId, String fromUser, Safeword safeword) {
        CoupleCatchSafewordPO existing = safewordMapper.find(spaceId, fromUser);
        if (existing == null) {
            safewordMapper.insert(CoupleCatchSafewordPO.of(spaceId, fromUser, safeword.word(), safeword.note()));
            return;
        }
        applyOwnedFields(existing, safeword);
        safewordMapper.updateById(existing);
    }

    /** 聚合负责维护的列：词与说明（改写安全词只动这两个），并刷新更新时间。 */
    private static void applyOwnedFields(CoupleCatchSafewordPO po, Safeword safeword) {
        po.setWord(safeword.word());
        po.setNote(safeword.note());
        po.setUpdatedAt(System.currentTimeMillis());
    }

    private static Safeword toDomain(CoupleCatchSafewordPO po) {
        return Safeword.restore(po.getId(), po.getSpaceId(), po.getFromUser(), po.getWord(), po.getNote());
    }
}
