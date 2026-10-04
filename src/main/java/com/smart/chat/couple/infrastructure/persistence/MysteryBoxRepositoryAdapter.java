package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.surprise.MysteryBox;
import com.smart.chat.couple.domain.surprise.MysteryBoxRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link MysteryBoxRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 列表沿用 {@link CoupleMysteryBoxMapper#findBySpace} 的「新→旧」排序，
 * {@code open_day} 在表里就是 yyyy-MM-dd 字符串，聚合按字符串比较日期，这里原样透传。
 * <p>
 * 写回<b>只碰聚合纳管的两列</b>（{@code opened}／{@code opened_at}）：盒子里的话、装盒人、
 * 开箱日装好即冻结，拿聚合重建整行会把它们静默清空。
 */
@Component
public class MysteryBoxRepositoryAdapter implements MysteryBoxRepository {

    private final CoupleMysteryBoxMapper boxMapper;

    public MysteryBoxRepositoryAdapter(CoupleMysteryBoxMapper boxMapper) {
        this.boxMapper = boxMapper;
    }

    @Override
    public List<MysteryBox> findBySpace(String spaceId) {
        return boxMapper.findBySpace(spaceId).stream().map(MysteryBoxRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<MysteryBox> findByIdIn(String id, String spaceId) {
        if (id == null || spaceId == null) {
            return Optional.empty();
        }
        CoupleMysteryBoxPO po = boxMapper.selectById(id);
        return po == null || !spaceId.equals(po.getSpaceId()) ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(MysteryBox box) {
        CoupleMysteryBoxPO existing = boxMapper.selectById(box.id());
        if (existing == null) {
            boxMapper.insert(toPo(box));
            return;
        }
        applyOwnedFields(existing, box);
        boxMapper.updateById(existing);
    }

    /** 聚合负责维护的列：拆没拆与拆解时刻。 */
    private static void applyOwnedFields(CoupleMysteryBoxPO po, MysteryBox box) {
        po.setOpened(box.opened());
        po.setOpenedAt(box.openedAt());
    }

    private static CoupleMysteryBoxPO toPo(MysteryBox box) {
        CoupleMysteryBoxPO po = new CoupleMysteryBoxPO();
        po.setId(box.id());
        po.setSpaceId(box.spaceId());
        po.setFromUser(box.fromUser());
        po.setKind(box.kind());
        po.setContent(box.content());
        po.setOpenDay(box.openDay());
        po.setOpened(box.opened());
        po.setOpenedAt(box.openedAt());
        po.setCreated(box.created());
        return po;
    }

    private static MysteryBox toDomain(CoupleMysteryBoxPO po) {
        return MysteryBox.restore(po.getId(), po.getSpaceId(), po.getFromUser(), po.getKind(), po.getContent(),
                po.getOpenDay(), po.isOpened(), po.getOpenedAt(), po.getCreated());
    }
}
