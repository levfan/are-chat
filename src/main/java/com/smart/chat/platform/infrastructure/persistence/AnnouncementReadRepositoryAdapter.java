package com.smart.chat.platform.infrastructure.persistence;

import com.smart.chat.platform.domain.announcement.AnnouncementRead;
import com.smart.chat.platform.domain.announcement.AnnouncementReadRepository;
import org.springframework.stereotype.Component;

/**
 * {@link AnnouncementReadRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 判重（一人一公告一行）沿用 Mapper 的 {@code exists}，不感知领域类型；
 * 追加走 insert，同 id 复查到原行时只回写聚合纳管的 {@code read_at}（username/announcement_id 不参与改写）。
 */
@Component
public class AnnouncementReadRepositoryAdapter implements AnnouncementReadRepository {

    private final AnnouncementReadMapper mapper;

    public AnnouncementReadRepositoryAdapter(AnnouncementReadMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean hasRead(String username, String announcementId) {
        return mapper.exists(username, announcementId);
    }

    @Override
    public void save(AnnouncementRead read) {
        AnnouncementReadPO existing = mapper.selectById(read.id());
        if (existing == null) {
            AnnouncementReadPO po = new AnnouncementReadPO();
            po.setId(read.id());
            po.setUsername(read.username());
            po.setAnnouncementId(read.announcementId());
            po.setReadAt(read.readAt());
            mapper.insert(po);
            return;
        }
        existing.setReadAt(read.readAt());
        mapper.updateById(existing);
    }
}
