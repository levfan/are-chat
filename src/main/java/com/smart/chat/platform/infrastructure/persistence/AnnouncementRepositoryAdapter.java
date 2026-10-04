package com.smart.chat.platform.infrastructure.persistence;

import com.smart.chat.platform.domain.announcement.Announcement;
import com.smart.chat.platform.domain.announcement.AnnouncementRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link AnnouncementRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 沿用 {@code CoupleSpaceRepositoryAdapter} 已确立的「只回写聚合纳管的列」纪律：聚合现在只会改
 * {@code enabled}（发布即冻结正文/发布人/发布时间，没有编辑用例），所以更新分支先 {@code selectById}
 * 取回原行，只回写 {@code enabled}，其余列原样留着。将来给公告加编辑用例，就在
 * {@link #applyOwnedFields} 里显式扩列，而不是靠聚合重建整行。
 */
@Component
public class AnnouncementRepositoryAdapter implements AnnouncementRepository {

    private final AnnouncementMapper mapper;

    public AnnouncementRepositoryAdapter(AnnouncementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Announcement> findActive() {
        return mapper.findLatestEnabled().map(AnnouncementRepositoryAdapter::toDomain);
    }

    @Override
    public List<Announcement> listRecent() {
        return mapper.findAllOrdered().stream().map(AnnouncementRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Announcement> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(AnnouncementRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Announcement announcement) {
        AnnouncementPO existing = mapper.selectById(announcement.id());
        if (existing == null) {
            mapper.insert(toPO(announcement));
            return;
        }
        applyOwnedFields(existing, announcement);
        mapper.updateById(existing);
    }

    /** 聚合负责维护的列：只有 enabled。 */
    private static void applyOwnedFields(AnnouncementPO po, Announcement announcement) {
        po.setEnabled(announcement.enabled());
    }

    private static AnnouncementPO toPO(Announcement announcement) {
        AnnouncementPO po = new AnnouncementPO();
        po.setId(announcement.id());
        po.setContent(announcement.content());
        po.setCreatedBy(announcement.createdBy());
        po.setEnabled(announcement.enabled());
        po.setCreated(announcement.created());
        return po;
    }

    private static Announcement toDomain(AnnouncementPO po) {
        return Announcement.restore(po.getId(), po.getContent(), po.getCreatedBy(), po.getEnabled(),
                po.getCreated());
    }
}
