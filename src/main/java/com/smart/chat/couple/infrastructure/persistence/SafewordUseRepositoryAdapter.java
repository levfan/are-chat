package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.safeword.SafewordUse;
import com.smart.chat.couple.domain.safeword.SafewordUseRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link SafewordUseRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleCatchSafewordUseMapper} 已有的 default 方法，排序口径与改造前逐字相同；
 * 归属闸门（「那次暂停是不是这个空间的」）也在这里用 {@code selectById} + 空间过滤复现原 {@code requireUse}。
 * <p>
 * {@link #save} 沿用 {@code CoupleSpaceRepositoryAdapter} 确立的「只回写聚合持有的列」纪律：
 * 新喊停整行插入（复盘落空串，与 {@link CoupleCatchSafewordUsePO#of} 一致）；复盘回填只动 reflect 与 updatedAt，
 * day / user_name / space_id / created 一律不碰。
 */
@Component
public class SafewordUseRepositoryAdapter implements SafewordUseRepository {

    private final CoupleCatchSafewordUseMapper useMapper;

    public SafewordUseRepositoryAdapter(CoupleCatchSafewordUseMapper useMapper) {
        this.useMapper = useMapper;
    }

    @Override
    public Optional<SafewordUse> findBySpaceAndDayAndUser(String spaceId, String user, String day) {
        return Optional.ofNullable(useMapper.find(spaceId, day, user)).map(SafewordUseRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<SafewordUse> findByIdInSpace(String spaceId, String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        CoupleCatchSafewordUsePO row = useMapper.selectById(id);
        if (row == null || !spaceId.equals(row.getSpaceId())) {
            return Optional.empty();
        }
        return Optional.of(toDomain(row));
    }

    @Override
    public List<SafewordUse> listBySpace(String spaceId) {
        return useMapper.findBySpace(spaceId).stream().map(SafewordUseRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(String spaceId, SafewordUse use) {
        CoupleCatchSafewordUsePO existing = useMapper.selectById(use.id());
        if (existing == null) {
            useMapper.insert(toNewPo(spaceId, use));
            return;
        }
        applyOwnedFields(existing, use);
        useMapper.updateById(existing);
    }

    /** 新喊停整行：复盘落空串、created 与 updatedAt 同刻——与 {@link CoupleCatchSafewordUsePO#of} 逐字一致。 */
    private static CoupleCatchSafewordUsePO toNewPo(String spaceId, SafewordUse use) {
        CoupleCatchSafewordUsePO po = new CoupleCatchSafewordUsePO();
        long now = System.currentTimeMillis();
        po.setId(use.id());
        po.setSpaceId(spaceId);
        po.setDay(use.day());
        po.setUserName(use.by());
        po.setReflect(use.reflect() == null ? "" : use.reflect());
        po.setCreated(now);
        po.setUpdatedAt(now);
        return po;
    }

    /** 聚合负责维护的列：复盘文字与更新时间。 */
    private static void applyOwnedFields(CoupleCatchSafewordUsePO po, SafewordUse use) {
        po.setReflect(use.reflect());
        po.setUpdatedAt(System.currentTimeMillis());
    }

    private static SafewordUse toDomain(CoupleCatchSafewordUsePO po) {
        return SafewordUse.restore(po.getId(), po.getDay(), po.getUserName(), po.getReflect());
    }
}
