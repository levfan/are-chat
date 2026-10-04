package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.anniversary.Anniversary;
import com.smart.chat.couple.domain.anniversary.AnniversaryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** {@link AnniversaryRepository} 的 MyBatis-Plus 适配器。 */
@Component
public class AnniversaryRepositoryAdapter implements AnniversaryRepository {

    private final CoupleAnniversaryMapper anniversaryMapper;

    public AnniversaryRepositoryAdapter(CoupleAnniversaryMapper anniversaryMapper) {
        this.anniversaryMapper = anniversaryMapper;
    }

    @Override
    public List<Anniversary> findBySpace(String spaceId) {
        return anniversaryMapper.findBySpace(spaceId).stream()
                .map(AnniversaryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Anniversary> findById(String id) {
        return Optional.ofNullable(anniversaryMapper.selectById(id)).map(AnniversaryRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Anniversary anniversary) {
        CoupleAnniversaryPO existing = anniversaryMapper.selectById(anniversary.id());
        if (existing == null) {
            anniversaryMapper.insert(toPo(anniversary));
        } else {
            anniversaryMapper.updateById(toPo(anniversary));
        }
    }

    @Override
    public void deleteById(String id) {
        anniversaryMapper.deleteById(id);
    }

    @Override
    public void deleteBySpace(String spaceId) {
        anniversaryMapper.deleteBySpace(spaceId);
    }

    private static Anniversary toDomain(CoupleAnniversaryPO po) {
        return Anniversary.restore(po.getId(), po.getSpaceId(), po.getTitle(), po.getEventDate(), po.getYearly(),
                po.getKind(), po.getCalendarType(), po.getLunarMd(), po.getCreatedBy(), po.getCreated());
    }

    private static CoupleAnniversaryPO toPo(Anniversary a) {
        CoupleAnniversaryPO po = new CoupleAnniversaryPO();
        po.setId(a.id());
        po.setSpaceId(a.spaceId());
        po.setTitle(a.title());
        po.setEventDate(a.eventDate());
        po.setYearly(a.repeatsYearly() ? 1 : 0);
        po.setKind(a.kind());
        po.setCalendarType(a.calendarType());
        po.setLunarMd(a.lunarMd());
        po.setCreatedBy(a.createdBy());
        po.setCreated(a.created());
        return po;
    }
}
