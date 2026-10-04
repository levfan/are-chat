package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.deed.Deed;
import com.smart.chat.couple.domain.deed.DeedRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link DeedRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询一律复用 {@link CoupleEchoDeedMapper} 已有的 default 方法（Mapper 不感知领域类型），
 * 排序口径因此与改造前逐字相同。
 * <p>
 * 写回沿用 {@code CoupleSpaceRepositoryAdapter} 确立的纪律：<b>只回写聚合纳管的那两列</b>
 * （{@code starred} 与 {@code updated_at}）。好事的内容、发生日、记录人一旦写下就不再有编辑用例，
 * 所以更新分支先 {@code selectById} 取回原行、改完再写回，绝不拿聚合重建整行。
 */
@Component
public class DeedRepositoryAdapter implements DeedRepository {

    private final CoupleEchoDeedMapper deedMapper;

    public DeedRepositoryAdapter(CoupleEchoDeedMapper deedMapper) {
        this.deedMapper = deedMapper;
    }

    @Override
    public Optional<Deed> findById(String deedId) {
        return Optional.ofNullable(deedMapper.selectById(deedId)).map(DeedRepositoryAdapter::toDomain);
    }

    @Override
    public List<Deed> listByRecorder(String spaceId, String fromUser) {
        return deedMapper.findByUser(spaceId, fromUser).stream().map(DeedRepositoryAdapter::toDomain).toList();
    }

    @Override
    public boolean alreadyRecorded(String spaceId, String fromUser, String day, String content) {
        return deedMapper.findByDayContent(spaceId, fromUser, day, content) != null;
    }

    @Override
    public void save(Deed deed) {
        CoupleEchoDeedPO existing = deedMapper.selectById(deed.id());
        if (existing == null) {
            deedMapper.insert(toPo(deed));
            return;
        }
        applyOwnedFields(existing, deed);
        deedMapper.updateById(existing);
    }

    /** 聚合负责维护的列：只有加星标记与更新时间。 */
    private static void applyOwnedFields(CoupleEchoDeedPO po, Deed deed) {
        po.setStarred(deed.starred() ? 1 : 0);
        po.setUpdatedAt(deed.updatedAt());
    }

    private static CoupleEchoDeedPO toPo(Deed deed) {
        CoupleEchoDeedPO po = new CoupleEchoDeedPO();
        po.setId(deed.id());
        po.setSpaceId(deed.spaceId());
        po.setFromUser(deed.fromUser());
        po.setContent(deed.content());
        po.setDay(deed.day());
        po.setStarred(deed.starred() ? 1 : 0);
        po.setCreated(deed.created());
        po.setUpdatedAt(deed.updatedAt());
        return po;
    }

    private static Deed toDomain(CoupleEchoDeedPO po) {
        return Deed.restore(po.getId(), po.getSpaceId(), po.getFromUser(), po.getContent(), po.getDay(),
                po.getStarred(), po.getCreated(), po.getUpdatedAt());
    }
}
