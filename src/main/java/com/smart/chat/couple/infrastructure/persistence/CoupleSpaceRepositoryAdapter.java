package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link CoupleSpaceRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 更新时<b>只回写聚合持有的那几列</b>：存储里还有 cityA/cityB 等聚合尚未纳管的字段，
 * 若用聚合重建整行就会把它们悄悄清空——所以先按 id 取回原行，改完再写回去。
 */
@Component
public class CoupleSpaceRepositoryAdapter implements CoupleSpaceRepository {

    private final CoupleSpaceMapper spaceMapper;

    public CoupleSpaceRepositoryAdapter(CoupleSpaceMapper spaceMapper) {
        this.spaceMapper = spaceMapper;
    }

    @Override
    public Optional<CoupleSpace> findActiveByMember(String username) {
        return spaceMapper.findActiveByUser(username).map(CoupleSpaceRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<CoupleSpace> findById(String id) {
        return Optional.ofNullable(spaceMapper.selectById(id)).map(CoupleSpaceRepositoryAdapter::toDomain);
    }

    @Override
    public List<CoupleSpace> findAllActive() {
        return spaceMapper.findAllActive().stream().map(CoupleSpaceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<CoupleSpace> findAll() {
        return spaceMapper.selectList(null).stream().map(CoupleSpaceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(CoupleSpace space) {
        CoupleSpacePO existing = spaceMapper.selectById(space.id());
        if (existing == null) {
            CoupleSpacePO po = new CoupleSpacePO();
            po.setId(space.id());
            po.setUserA(space.userA());
            po.setUserB(space.userB());
            po.setStatus(space.status());
            po.setCreated(space.created());
            applyOwnedFields(po, space);
            spaceMapper.insert(po);
            return;
        }
        applyOwnedFields(existing, space);
        spaceMapper.updateById(existing);
    }

    /** 聚合负责维护的列；其余列（cityA/cityB…）由各自的写入方负责，这里一律不碰 */
    private static void applyOwnedFields(CoupleSpacePO po, CoupleSpace space) {
        po.setStatus(space.status());
        po.setAnniversary(space.anniversary());
        po.setNickA(space.nickA());
        po.setNickB(space.nickB());
        po.setSlogan(space.slogan());
        po.setTheme(space.theme());
        po.setStickers(space.stickers());
        po.setDissolvedAt(space.dissolvedAt());
    }

    private static CoupleSpace toDomain(CoupleSpacePO po) {
        return CoupleSpace.restore(po.getId(), po.getUserA(), po.getUserB(), po.getStatus(),
                po.getCreated() == null ? 0L : po.getCreated(), po.getAnniversary(),
                po.getNickA(), po.getNickB(), po.getSlogan(), po.getTheme(), po.getStickers(), po.getDissolvedAt());
    }
}
