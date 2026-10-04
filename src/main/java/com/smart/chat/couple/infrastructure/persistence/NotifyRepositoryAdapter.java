package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.notify.NotifyEntry;
import com.smart.chat.couple.domain.notify.NotifyRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** {@link NotifyRepository} 的 MyBatis-Plus 适配器。 */
@Component
public class NotifyRepositoryAdapter implements NotifyRepository {

    private final CoupleNotifyMapper notifyMapper;

    public NotifyRepositoryAdapter(CoupleNotifyMapper notifyMapper) {
        this.notifyMapper = notifyMapper;
    }

    @Override
    public List<NotifyEntry> findRecent(String username) {
        return notifyMapper.findMine(username).stream().map(NotifyRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countUnread(String username) {
        return notifyMapper.countUnread(username);
    }

    @Override
    public int markAllRead(String username) {
        return notifyMapper.markAllRead(username);
    }

    @Override
    public void append(NotifyEntry entry) {
        CoupleNotifyPO po = new CoupleNotifyPO();
        po.setId(entry.id());
        po.setUsername(entry.username());
        po.setEvent(entry.event());
        po.setActor(entry.actor());
        po.setDetail(entry.detail());
        po.setReadFlag(entry.readFlag());
        po.setCreated(entry.created());
        notifyMapper.insert(po);
    }

    private static NotifyEntry toDomain(CoupleNotifyPO po) {
        return NotifyEntry.restore(po.getId(), po.getUsername(), po.getEvent(), po.getActor(), po.getDetail(),
                po.getReadFlag(), po.getCreated());
    }
}
